package com.devmh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(indexName = "case-changelog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseChangeLog {
    @Id
    private UUID id;
    private UUID caseId;
    private List<String> changes;
    private Instant timestamp;
}