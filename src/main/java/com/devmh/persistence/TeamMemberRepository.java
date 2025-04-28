package com.devmh.persistence;

import com.devmh.model.TeamMember;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.UUID;

public interface TeamMemberRepository extends Neo4jRepository<TeamMember, UUID> {

    List<TeamMember> findByNameContainingIgnoreCase(String name);
}
