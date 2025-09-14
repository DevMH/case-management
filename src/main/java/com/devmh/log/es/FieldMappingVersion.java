package com.devmh.log.es;

import com.devmh.log.model.FieldDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldMappingVersion {
    private String id; // mappingKey:version
    private String mappingKey; // e.g., "case"
    // document version
    private long version;
    private String description;

    private LinkedHashMap<String, FieldDescriptor> descriptors = new LinkedHashMap<>();

}
