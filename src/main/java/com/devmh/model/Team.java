package com.devmh.model;

import lombok.Data;
import java.util.Set;

@Data
class Team {
    private Set<User> members;

    public Team(Set<User> members) {
        this.members = members;
    }
}
