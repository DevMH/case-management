package com.devmh.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public interface FieldDescriptorProvider {

    @JsonIgnore
    public String getRootFieldName();

    @JsonIgnore
    public Map<String, FieldDescriptor> getFieldDescriptors();
}
