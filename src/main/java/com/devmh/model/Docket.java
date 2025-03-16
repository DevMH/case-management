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
@Document(indexName = "cases")
public class Docket {
    @Id
    private UUID id;
    private String name;

    public Docket(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }
}
