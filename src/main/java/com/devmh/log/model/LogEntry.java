package com.devmh.log.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntry {
    private String id;
    private Instant timestamp;
    private String user;
    private String mappingKey;
    private long mappingVersion;
    private JsonNode patchOps; // RFC6902 array
}
