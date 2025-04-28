package com.devmh.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "legacy_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyCase {
    @Id
    private Long id;
    private String name;
    private String state; // or ApprovalState if mapped properly
}

