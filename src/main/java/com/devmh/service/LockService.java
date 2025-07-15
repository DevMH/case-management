package com.devmh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LockService {

    private final StringRedisTemplate redisTemplate;

    public boolean acquireLock(String key, Duration ttl) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String lockValue = UUID.randomUUID().toString();
        Boolean success = ops.setIfAbsent(key, lockValue, ttl);
        return Boolean.TRUE.equals(success);
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    public String getLockOwner(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails user) {
            return user.getUsername();
        } else if (auth != null) {
            return auth.getName();
        }
        return "unknown";
    }
}
