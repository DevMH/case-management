package com.devmh.log.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldMapping {
    private String id; // mappingKey:version
    private String mappingKey; // e.g., "case"
    private long version;
    private String description;

    private LinkedHashMap<String, FieldDescriptor> descriptors = new LinkedHashMap<>();

}
