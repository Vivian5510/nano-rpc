# Nano-RPC 架构设计文档

## 1. 项目模块规划

- **nano-transport**: Netty 网络层封装。负责连接管理、编解码、基础通信能力。
- **nano-protocol-rpc**: RPC 协议层实现。负责序列化、RPC 请求/响应协议定义、服务调用逻辑。

## 2. Command 与 Processor 扩展性设计

为了应对 Command 种类的频繁变化并保持架构的优雅（符合开闭原则 OCP），我们采用 **“接口化抽象” + “自动注册”** 的方案。

### 2.1 核心抽象接口

避免使用枚举（Enum）硬编码命令码，而是使用接口。

```java
// 命令码接口，允许业务层随意扩展
public interface CommandCode {
    short value();
}

// 处理器接口
public interface RemotingProcessor<T extends RemotingCommand> {
    void process(RemotingContext ctx, T msg) throws Exception;
    CommandCode getCmdCode(); // 标识该处理器处理哪种命令
}
```

### 2.2 请求与响应的差异化处理

|特性 |Request (请求) |Response (响应) |
|:---|:---|:---|
|**核心目标** |分发业务逻辑并执行 |匹配历史请求并唤醒/回调 |
|**流转路径** |Decoder -> ProcessorManager -> **UserProcessor** |Decoder -> ProcessorManager -> **ResponseProcessor** |
|**线程模型** |**必须切换到业务线程池** (避免阻塞 IO 线程) |通常在 **IO 线程** 或 **轻量级回调线程池** 处理 |
|**后续动作** |执行完毕后，构建 Response 写回 Channel |找到 `InvokeFuture`，设置结果，`countDown` 唤醒主线程 |

## 3. 终极优雅方案：基于编译时注解的自动注册 (APT + JavaPoet)

为了解决“每新增一个指令都需要手动修改启动类进行注册”的痛点，我们引入 **APT (Annotation Processing Tool)** 技术。

### 3.1 核心流程


1. **定义注解**: 创建 `@RpcCmd` 注解，标记在 Processor 实现类上。
2. **编写 Processor**:

   ```java
   @RpcCmd(code = 1001)
   public class LoginProcessor implements RemotingProcessor { ... }
   ```
3. **编译期处理 (APT)**:
   - 编写 `AnnotationProcessor` 扫描所有带有 `@RpcCmd` 的类。
   - 使用 **JavaPoet** 库自动生成注册代码（源码级）。
4. **自动生成的代码 (AutoRegistry.java)**:

   ```java
   // 编译后自动生成在 target 目录，无需手写
   public class AutoRegistry {
       public static void registerAll(ProcessorManager manager) {
            manager.registerProcessor(1001, new com.myapp.LoginProcessor());
            // ... 其他自动发现的处理器
       }
   }
   ```
5. **启动调用**:

   ```java
   // 只有这一行代码是静态的
   AutoRegistry.registerAll(server.getProcessorManager());
   ```

### 3.2 依赖技术栈

- **Java APT (AbstractProcessor)**: JDK 自带，用于挂载编译期逻辑。
- **JavaPoet**: Square 公司开源库，用于优雅地生成 `.java` 源文件（自动处理导包、格式化）。

### 3.3 优势

- **零耦合**: 不依赖 Spring 等第三方容器。
- **高性能**: 纯静态代码注册，无运行时反射或扫描开销。
- **易维护**: 开发者只关注 Processor 开发，注册逻辑全自动。


