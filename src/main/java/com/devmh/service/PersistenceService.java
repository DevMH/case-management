package com.devmh.service;

// Combined service to orchestrate persistence of Case and Team
// 1. Save all TeamMember nodes and relationships first
//    List<TeamMember> members = ...;
//    teamMemberRepository.saveAll(members);

// 2. Save the Case with a reference to the team (e.g. by UUID or mapped relationship if embedded)
//    Case c = new Case(...);
//    c.setTeamId(members.stream().map(TeamMember::getId).collect(Collectors.toList()));
//    caseRepository.save(c);

// If modeling Case -> Team graph relationship directly:
// @Relationship(type = "HAS_TEAM")
// private List<TeamMember> team;

// Ensure cascading and fetch modes are managed if bidirectional

import com.devmh.model.Case;
import com.devmh.model.TeamMember;
import com.devmh.persistence.CaseRepository;
import com.devmh.persistence.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class PersistenceService {

    private final CaseRepository caseRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public void saveCaseWithTeam(Case caseEntity, List<TeamMember> teamMembers) {
        try {
            // Save team members to Neo4j
            List<TeamMember> savedTeam = teamMemberRepository.saveAll(teamMembers);

            // Optionally map team members back into case entity if there's a graph reference
            // caseEntity.setTeam(savedTeam); // or store their IDs if linked by reference

            // Save case to Elasticsearch
            caseRepository.save(caseEntity);

        } catch (Exception ex) {
            // Optional cleanup if partial data should not persist
            teamMemberRepository.deleteAll(teamMembers);
            throw new RuntimeException("Failed to persist case and team: " + ex.getMessage(), ex);
        }
    }
}
