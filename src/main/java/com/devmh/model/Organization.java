package com.devmh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "organizations")
public class Organization {
    @Id
    private UUID id;
    private String name;
    private Organization parent;

    public Organization(String name, Organization parent) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.parent = parent;
    }
}
