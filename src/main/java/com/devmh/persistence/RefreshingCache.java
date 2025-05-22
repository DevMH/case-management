package com.devmh.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class RefreshingCache {

    private volatile Map<String, Object> cache;

    @PostConstruct
    @Scheduled(fixedRateString = "${cache.refresh.interval:60000}")
    private void loadCache() {
        cache = Map.of("A","1", "B", "2");
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public List<Object> getAll() {
        return new ArrayList<>(cache.values());
    }
}
