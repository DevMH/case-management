package com.devmh.persistence;

import com.devmh.model.Case;
import com.devmh.model.LegacyCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LegacyCaseMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    Case toElasticsearchCase(LegacyCase legacy);

    //LegacyCaseMapper INSTANCE = Mappers.getMapper(LegacyCaseMapper.class);

}
