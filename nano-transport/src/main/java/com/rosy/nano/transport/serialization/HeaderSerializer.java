package com.rosy.nano.transport.serialization;

import com.rosy.nano.transport.command.CommandHeadDTO;

public interface HeaderSerializer {

    HeaderSerializeType type();

    byte[] encode(CommandHeadDTO dto);

    CommandHeadDTO decode(byte[] headerBytes);

}
