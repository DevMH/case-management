package com.devmh.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.UUID;

@Data
@Document(indexName = "dockets")
class Docket {
    @Id
    private UUID id;
    private String name;

    public Docket(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }
}
