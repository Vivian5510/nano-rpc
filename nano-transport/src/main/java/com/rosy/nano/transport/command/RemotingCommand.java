package com.rosy.nano.transport.command;


import com.google.common.base.Preconditions;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import com.rosy.nano.transport.serialization.HeaderSerializerRegistry;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>网络帧格式：
 * <p>[ totalLen:int ]
 * <p>[ headerLenType:int ]   // 高8位=HeaderSerializeType，低24位=headerLen
 * <p>[ headerBytes:headerLen ]
 * <p>[ bodyBytes: totalLen - 4 - headerLen ]
 */
@Getter
@Setter
public class RemotingCommand {

    private static final int HEADER_LEN_MASK = 0x00FF_FFFF;

    private static final int SERIALIZE_TYPE_MASK = 0xFF;

    private static final String REMOTING_VERSION_KEY = "nano.remoting.version";
    private static final AtomicLong REQUEST_ID = new AtomicLong(0);
    private static volatile int configVersion = -1;
    private int code;

    private int version;

    @Setter(AccessLevel.NONE)
    private long opaque;

    @Setter(AccessLevel.NONE)
    private int flag;

    private String remark;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Map<String, String> extFields;

    private HeaderSerializeType headerSerializeType;

    private transient CustomCommandHeader header;
    private transient byte[] body;

    private RemotingCommand() {
    }

    private static int currentVersion() {
        if (configVersion >= 0) return configVersion;
        String v = System.getProperty(REMOTING_VERSION_KEY);
        if (v != null) {
            configVersion = Integer.parseInt(v);
            return configVersion;
        }
        return 0;
    }

    public static RemotingCommand newRequest(int code, CustomCommandHeader header) {
        return newRequest(code, header, HeaderSerializeType.JSON);
    }

    public static RemotingCommand newRequest(int code, CustomCommandHeader header, HeaderSerializeType type) {
        RemotingCommand request = new RemotingCommand();
        request.code = code;
        request.opaque = REQUEST_ID.getAndIncrement();
        request.header = header;
        request.headerSerializeType = type;
        request.version = currentVersion();
        return request;
    }

    public static RemotingCommand newResponse(int code, RemotingCommand request, CustomCommandHeader header) {
        return newResponse(code, request, header, HeaderSerializeType.JSON);
    }

    public static RemotingCommand newResponse(int code, RemotingCommand request, CustomCommandHeader header, HeaderSerializeType type) {
        RemotingCommand response = new RemotingCommand();
        response.code = code;
        response.opaque = request.getOpaque();
        response.header = header;
        response.headerSerializeType = type;
        response.version = currentVersion();
        response.markResponse();
        return response;
    }

    public static RemotingCommand decode(ByteBuf frame) {
        int headerLenType = frame.readInt();
        int headerLen = getHeaderLen(headerLenType);
        HeaderSerializeType type = getHeaderSerializeType(headerLenType);

        if (headerLen > frame.readableBytes()) throw new IllegalArgumentException("bad headerLen=" + headerLen);

        byte[] headerBytes = new byte[headerLen];
        frame.readBytes(headerBytes);

        RemotingCommand command = headerDecode(headerBytes, type);

        if (frame.readableBytes() > 0) {
            byte[] body = new byte[frame.readableBytes()];
            frame.readBytes(body);
            command.body = body;
        }

        return command;
    }

    private static RemotingCommand headerDecode(byte[] headerBytes, HeaderSerializeType type) {
        RemotingCommand command = new RemotingCommand();
        CommandHeadDTO dto = HeaderSerializerRegistry.get(type).decode(headerBytes);
        command.code = dto.getCode();
        command.version = dto.getVersion();
        command.opaque = dto.getOpaque();
        command.flag = dto.getFlag();
        command.remark = dto.getRemark();
        command.extFields = dto.getExtFields();
        command.headerSerializeType = type;
        return command;
    }

    public static int calHeaderLenType(int headerLen, HeaderSerializeType type) {
        return (headerLen & HEADER_LEN_MASK) | ((type.getCode() & SERIALIZE_TYPE_MASK) << 24);
    }

    public static int getHeaderLen(int headerLenType) {
        return headerLenType & HEADER_LEN_MASK;
    }

    public static HeaderSerializeType getHeaderSerializeType(int headerLenType) {
        return HeaderSerializeType.from((headerLenType >>> 24) & SERIALIZE_TYPE_MASK);
    }

    public Map<String, String> getOrCreateExtFields() {
        if (extFields == null) extFields = new HashMap<>();
        return extFields;
    }

    public Map<String, String> getExtFieldsQuietly() {
        return extFields;
    }

    public void encode(ByteBuf out) {
        encodeHeader(); // custom header -> extFields

        byte[] headerBytes = headerEncode();
        int bodyLen = body == null ? 0 : body.length;
        int totalLen = 4 + headerBytes.length + bodyLen;

        out.writeInt(totalLen);
        out.writeInt(calHeaderLenType(headerBytes.length, headerSerializeType));
        out.writeBytes(headerBytes);
        if (bodyLen != 0) out.writeBytes(body);
    }

    private byte[] headerEncode() {
        CommandHeadDTO dto = CommandHeadDTO.builder()
                .code(code)
                .opaque(opaque)
                .flag(flag)
                .version(version)
                .remark(remark)
                .extFields(extFields)
                .build();

        return HeaderSerializerRegistry.get(headerSerializeType).encode(dto);
    }

    public void encodeHeader() {
        if (header != null) header.encodeTo0(getOrCreateExtFields());
    }

    public <T extends CustomCommandHeader> T decodeHeader(T h) {
        Preconditions.checkArgument(h != null, "header can't be null when decoding");
        h.decodeFrom0(extFields);
        this.header = h;
        return h;
    }

    public void markResponse() {
        flag = Bits.RESPONSE.set(flag);
    }

    public boolean isResponse() {
        return Bits.RESPONSE.get(flag);
    }

    public void markOneway() {
        flag = Bits.ONEWAY.set(flag);
    }

    public boolean isOneWay() {
        return Bits.ONEWAY.get(flag);
    }

    private enum Bits {
        RESPONSE(0, "0=request, 1=response"),
        ONEWAY(1, "0=two-way, 1=one-way");

        final int bit;
        final int mask;
        final String meaning;

        Bits(int bit, String meaning) {
            this.bit = bit;
            this.mask = 1 << bit;
            this.meaning = meaning;
        }

        boolean get(int flag) {
            return (flag & mask) != 0;
        }

        int set(int flag) {
            return flag | mask;
        }
    }
}
