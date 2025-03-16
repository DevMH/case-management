package com.devmh.persistence;

import com.devmh.model.Case;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.UUID;

public interface CaseRepository extends ElasticsearchRepository<Case, UUID> {}
