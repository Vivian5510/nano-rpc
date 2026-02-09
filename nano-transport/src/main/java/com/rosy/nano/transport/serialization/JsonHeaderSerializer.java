package com.rosy.nano.transport.serialization;

import com.google.auto.service.AutoService;
import com.google.common.base.Preconditions;
import com.rosy.nano.transport.command.CommandHeadDTO;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@AutoService(HeaderSerializer.class)
public class JsonHeaderSerializer implements HeaderSerializer {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            .build();

    @Override
    public HeaderSerializeType type() {
        return HeaderSerializeType.JSON;
    }

    @Override
    public byte[] encode(CommandHeadDTO dto) {
        Preconditions.checkArgument(dto != null, "CommandHeadDTO can't be null when encoding");
        return MAPPER.writeValueAsBytes(dto);
    }

    @Override
    public CommandHeadDTO decode(byte[] headerBytes) {
        Preconditions.checkArgument(headerBytes != null, "HeaderBytes can't be null when decoding");
        return MAPPER.readValue(headerBytes, CommandHeadDTO.class);
    }
}
