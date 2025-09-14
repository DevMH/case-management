package com.devmh.log.model;

import java.util.Map;

public interface FieldDescriptorProvider {

    Map<String, FieldDescriptor> getFieldDescriptors();
}
