package com.devmh.log.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.devmh.log.model.LogEntry;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ElasticLogRepository {

    private final ElasticsearchClient es;
    private final String index;

    public ElasticLogRepository(ElasticsearchClient es, String index) {
        this.es = es;
        this.index = index;
    }

    public LogEntry save(LogEntry e) throws IOException {
        if (e.getId() == null) e.setId(UUID.randomUUID().toString());
        if (e.getTimestamp() == null) e.setTimestamp(Instant.now());
        IndexRequest<LogEntry> req = IndexRequest.of(b -> b
                .index(index).id(e.getId()).document(e).refresh(Refresh.WaitFor));
        es.index(req);
        return e;
    }

    public Optional<LogEntry> findById(String id) throws IOException {
        GetResponse<LogEntry> resp =
                es.get(GetRequest.of(b -> b.index(index).id(id)), LogEntry.class);
        return resp.found() ? Optional.of(resp.source()) : Optional.empty();
    }
}

/*
1: Elasticsearch index mapping
PUT /log_entry
{
  "mappings": {
    "properties": {
      "timestamp": {
        "type": "date"
      },
      "user_id": {
        "type": "keyword"
      },
      "target_document_id": {
        "type": "keyword"
      },
      "patch_operations": {
        "type": "nested"
      },
      "before_state": {
        "type": "object",
        "enabled": false
      },
      "after_state": {
        "type": "object",
        "enabled": false
      }
    }
  }
}
2: example document
{
  "timestamp": "2025-09-13T12:00:00Z",
  "user_id": "user123",
  "target_document_id": "doc456",
  "patch_operations": [
    { "op": "replace", "path": "/name", "value": "Alice" },
    { "op": "add", "path": "/last_modified_by", "value": "user123" }
  ]
}
 */
