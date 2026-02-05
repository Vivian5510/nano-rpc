# Nano-RPC（Bolt 风格）MVP 方案与执行计划

> 目标：在 `nano-rpc` 当前两模块结构下，按 SOFA-Bolt 的核心思路实现一套最小可跑通的 RPC：
>
> - 路由：按 `requestClass`（`UserProcessor.interest()`）分发
> - 客户端：`CompletableFuture` 异步 + 同步封装
> - 服务端：同步处理 + `AsyncContext` 异步回包
> - 序列化：JSON（但协议头预留 `serializerId`，未来可替换）


---

## 1. 总览（架构分层与依赖方向）

### 1.1 模块边界

- `nano-transport`：通用 Remoting 底座（生命周期、连接、命令、编解码、分发、出站调用引擎）。
- `nano-protocol-rpc`：RPC 协议实现（UserProcessor、RPC 命令、RPC processors、RpcServer/RpcClient 门面、JSON 序列化实现）。

依赖方向（强约束）：

- `nano-protocol-rpc` 只能依赖 `nano-transport`
- `nano-transport` 不能依赖任何“协议具体实现”

### 1.2 最小调用链（端到端）

**Client 出站（invokeAsync）**


1. request POJO → `RpcRequestCommand`（携带 `requestClass` + JSON payload）
2. `BaseRemoting.invokeAsync`：挂起 `pending(requestId -> CompletableFuture)` → `writeAndFlush` → 超时/发送失败处理
3. 收到 `RpcResponseCommand` → `RpcResponseProcessor` 按 `requestId` complete future

**Server 入站（请求处理 + 回包）**


1. Netty decode 得到 `RpcRequestCommand`
2. `RpcRequestProcessor`：`requestClass` → 找 `UserProcessor` → 投递到业务线程池
3. 同步处理器：直接返回 response POJO → 编码为 `RpcResponseCommand` → 回写
4. 异步处理器：通过 `RpcAsyncContext.sendResponse(resp)` 在任意时刻回写（只允许一次回写）

### 1.3 与 Bolt 源码对照（学习坐标）

- 生命周期骨架：`AbstractRemotingServer`（Bolt：`.../AbstractRemotingServer.java`）
- 出站调用引擎：`BaseRemoting`（Bolt：`.../BaseRemoting.java`）
- RPC 出站封装：`RpcRemoting extends BaseRemoting`（Bolt：`.../rpc/RpcRemoting.java`）
- 入站分发：`RpcHandler -> RpcCommandHandler -> ProcessorManager`（Bolt：`.../rpc/RpcHandler.java`、`.../rpc/protocol/RpcCommandHandler.java`、`.../ProcessorManager.java`）
- 请求/响应处理器：`RpcRequestProcessor`、`RpcResponseProcessor`（Bolt：`.../rpc/protocol/RpcRequestProcessor.java`、`.../rpc/protocol/RpcResponseProcessor.java`）
- 请求/响应命令层次：`RequestCommand/ResponseCommand` 与 `RpcRequestCommand`（Bolt：`.../rpc/RequestCommand.java`、`.../rpc/ResponseCommand.java`、`.../rpc/protocol/RpcRequestCommand.java`）


---

## 2. 关键决策（MVP 固化，后续可扩展）

### 2.1 路由模型

- 仅实现 Bolt 路线：按 `requestClass`（字符串）路由到 `UserProcessor.interest()`。
- 不实现 `serviceName + methodName`（接口式 RPC）。

### 2.2 异步的两种含义（必须区分）

- **客户端异步**：`invokeAsync` 返回 `CompletableFuture`（等待响应）。
- **服务端异步**：`AsyncUserProcessor` 使用 `AsyncContext` 延迟回包。

两者可同时存在，互不替代。

### 2.3 编解码与序列化

- Wire 协议头携带 `serializerId`，MVP 先实现 JSON（Jackson）。
- 未来替换为 Kryo/Protobuf 等：新增 `Serializer` 实现 + 新 `serializerId`，不改上层 API。

### 2.4 requestId 选择

- `requestId` 使用 **long**（避免高并发/长时间运行时的 wrap-around 风险；与 future 映射更安全）。

### 2.5 线程模型（MVP）

- 请求业务逻辑默认不在 IO 线程执行：`RpcRequestProcessor` 投递到业务线程池。
- `processInIOThread()` 先保留接口但默认 false（MVP 不开放/不推荐）。


---

## 3. 核心接口与类（建议最小清单）

### 3.1 nano-transport（通用层）

- 生命周期：
  - `LifeCycle`：`startup()` / `shutdown()`
  - `RemotingServer`、`RemotingClient`
  - `AbstractRemotingServer`、`AbstractRemotingClient`（模板方法：`doInit/doStart/doStop`）
- 命令与分发：
  - `CommandCode`（short/int）
  - `RemotingCommand`（统一信封：`cmdCode`、`requestId`、`protocolId`、`serializerId`、`flags`、`bodyBytes`）
  - `RequestCommand extends RemotingCommand`（`timeoutMs`、`oneway`）
  - `ResponseCommand extends RemotingCommand`（`status`、`error`）
  - `RemotingProcessor<T>`、`AbstractRemotingProcessor<T>`、`ProcessorManager`
  - `CommandHandler`（持有 `ProcessorManager` 并分发）
  - `RemotingContext`（封装 channel/writeAndFlush、附带用户处理器映射等必要上下文）
- 连接与出站调用：
  - `Connection`（持有 Netty channel、`pending` map）
  - `BaseRemoting`（`invokeAsync/invokeSync/oneway`，完成 pending、timeout、send-fail）
- 协议插槽：
  - `Protocol`（`protocolId` + `Codec` + `CommandHandler`）
  - `Codec`（Netty encoder/decoder）
  - `NettyRemotingServer`、`NettyRemotingClient`（只做启动与 pipeline 组装，协议通过插槽注入）

### 3.2 nano-protocol-rpc（RPC 层）

- 用户扩展点：
  - `UserProcessor<T>`（`interest()`、`getExecutor()`、`processInIOThread()`、`preHandleRequest(...)`）
  - `SyncUserProcessor<T>`：`Object handleRequest(BizContext, T) throws Exception`
  - `AsyncUserProcessor<T>`：`void handleRequest(BizContext, AsyncContext, T)`
- RPC 命令：
  - `RpcRequestCommand extends RequestCommand`：`requestClass` + JSON payload
  - `RpcResponseCommand extends ResponseCommand`：`success` + `responseClass` + JSON payload / error
- RPC 分发与处理器：
  - `RpcCommandHandler`：注册 `RPC_REQUEST` / `RPC_RESPONSE`
  - `RpcRequestProcessor`：路由、线程池投递、同步/异步回包
  - `RpcResponseProcessor`：按 `requestId` complete client future
  - `RpcAsyncContext`：`sendResponse(Object)`（保证只回一次）
- RPC 门面：
  - `RpcServer`：注册 processors/userProcessors；启动底层 `NettyRemotingServer`
  - `RpcClient`：`invokeAsync/invokeSync`；启动底层 `NettyRemotingClient`
- JSON：
  - `JsonSerializer implements Serializer`


---

## 4. 分步执行计划（5 个阶段，每阶段可验收）

> 原则：每个阶段完成后都应当“可编译 + 可运行一个最小 demo”，避免后期大爆炸。

### 阶段 1：Transport 生命周期与分发骨架

交付物（`nano-transport`）：

- `LifeCycle`、`RemotingServer/Client`、`AbstractRemotingServer/Client`
- `CommandCode`、`RemotingCommand`、`RequestCommand`、`ResponseCommand`
- `RemotingProcessor`、`AbstractRemotingProcessor`、`ProcessorManager`
- `RemotingContext`

验收：

- 可以注册一个 dummy processor（基于 cmdCode）并在单元测试/本地 main 中调用其 `process`（不要求网络）。

### 阶段 2：Netty Transport + 通用帧协议（codec）

交付物（`nano-transport`）：

- `Protocol` / `Codec` / `CommandHandler` 插槽
- `NettyRemotingServer`、`NettyRemotingClient`
- 通用帧协议（建议字段：magic/version/protocolId/cmdCode/serializerId/flags/requestId/bodyLen/body）

验收：

- client 发送一个 `RemotingCommand`，server 能 decode 并进到 `CommandHandler`。

### 阶段 3：BaseRemoting（CompletableFuture 出站调用引擎）

交付物（`nano-transport`）：

- `Connection` 的 `pending` 映射（`requestId -> CompletableFuture<ResponseCommand>`）
- `BaseRemoting.invokeAsync/invokeSync/oneway`（超时与发送失败处理）
- `ResponseProcessor` 能 complete pending（先用 dummy cmdCode 验证）

验收：

- client 侧 `invokeAsync` 在收到 server 返回的 response 后能完成 future；
- 超时能触发 completeExceptionally 且 `pending` 不泄漏。

### 阶段 4：RPC 协议（按 requestClass 路由）+ JSON

交付物（`nano-protocol-rpc`）：

- `UserProcessor` + `SyncUserProcessor` + `AsyncUserProcessor` + `AsyncContext`
- `RpcRequestCommand` / `RpcResponseCommand`
- `JsonSerializer`
- `RpcCommandHandler`、`RpcRequestProcessor`、`RpcResponseProcessor`、`RpcAsyncContext`

验收：

- server 注册两个 processor：
  - 一个同步：立即返回 response
  - 一个异步：延迟回包（`asyncCtx.sendResponse`）
- client 对两类请求都能 `invokeAsync` 拿到正确响应。

### 阶段 5：RpcServer/RpcClient 门面与 Demo 固化

交付物（`nano-protocol-rpc` + demo）：

- `RpcServer`：持有 `ConcurrentHashMap<String, UserProcessor<?>>` 并注入到 `RemotingContext`
- `RpcClient`：提供 `invokeAsync/invokeSync`（同步基于 future.get(timeout)）
- 一个可运行 demo（单 JVM 启动 server，再跑 client）

验收：

- 端到端示例在本机可运行：10 次请求成功，包含一次异步回包请求。


---

## 5. 第二阶段（非 MVP，但为扩展预留的钩子）

- `serializerId` 扩展：Kryo/Protobuf
- `processInIOThread` 分级反序列化（类似 Bolt 的 DESERIALIZE_CLAZZ/HEADER/ALL）
- 心跳、连接管理、重连
- APT 自动注册 processors（参考 `docs/design/command_processor_architecture.md` 的方案）


