package com.devmh.persistence;

import com.devmh.model.CaseChangeLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface CaseChangeLogRepository extends ElasticsearchRepository<CaseChangeLog, UUID> {}
