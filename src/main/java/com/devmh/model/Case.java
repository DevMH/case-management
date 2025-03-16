package com.devmh.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.List;
import java.util.UUID;

@Data
@Document(indexName = "cases")
public class Case {
    @Id
    private UUID id;
    private String name;
    private Docket docket;
    private ApprovalState state;
    private Team team;
    private Location location;
    private Organization organization;
    private List<Finding> findings;
    private Timetable timetable;

    public Case(String name, Docket docket) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.docket = docket;
        this.state = ApprovalState.PENDING;
    }
}
