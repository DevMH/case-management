package com.devmh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
public class User {
    @Id
    private UUID id;
    private String name;
    private Role role;
    private Set<Organization> organizations;

    public User(String name, Role role, Set<Organization> organizations) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.role = role;
        this.organizations = organizations;
    }
}
