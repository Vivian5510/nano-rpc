package com.rosy.nano.transport.command;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandHeadDTO {
    private int code;

    private int version;

    private long opaque;

    private int flag;

    private String remark;

    private Map<String, String> extFields;
}
