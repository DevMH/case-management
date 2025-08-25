package com.devmh.model;

import com.devmh.util.FieldDescriptor;
import com.devmh.util.FieldDescriptorProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "cases")
public class Case implements FieldDescriptorProvider {
    @Id
    private UUID id;
    @Version
    private Long version;
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

    @Override
    public String getRootFieldName() {
        return "case";
    }

    @Override
    public Map<String, FieldDescriptor> getFieldDescriptors() {
        return FIELD_DESCRIPTORS;
    }

    public static final Map<String, FieldDescriptor> FIELD_DESCRIPTORS = new HashMap<>();
    static {
        FIELD_DESCRIPTORS.put("case", new FieldDescriptor("case", false, "case's"));
        FIELD_DESCRIPTORS.put("case/team", new FieldDescriptor("case team", false, "case team's"));
        FIELD_DESCRIPTORS.put("case/findings", FieldDescriptor.collection("findings"));
        FIELD_DESCRIPTORS.put("case/team/teamMember[]", FieldDescriptor.collection("team members"));
        FIELD_DESCRIPTORS.put("case/name", FieldDescriptor.scalar("name"));
        FIELD_DESCRIPTORS.put("case/state", FieldDescriptor.scalar("approval state"));
        FIELD_DESCRIPTORS.put("case/docket/name", FieldDescriptor.scalar("docket name"));
        FIELD_DESCRIPTORS.put("case/team/teamMember[]/firstName", FieldDescriptor.scalar("first name"));
        FIELD_DESCRIPTORS.put("case/team/teamMember[]/lastName", FieldDescriptor.scalar("last name"));
        FIELD_DESCRIPTORS.put("case/findings[]/startDate", FieldDescriptor.scalar("finding start date"));
        FIELD_DESCRIPTORS.put("case/findings[]/endDate", FieldDescriptor.scalar("finding end date"));
        FIELD_DESCRIPTORS.put("case/findings[]/sensitive", FieldDescriptor.scalar("finding sensitivity"));
    }
}
