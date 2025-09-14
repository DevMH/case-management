package com.devmh.log.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.devmh.log.model.FieldMapping;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Instant;

@RequiredArgsConstructor
public class ElasticFieldMappingRepository {

    private final ElasticsearchClient es;
    private final String index;

    public FieldMapping save(FieldMapping mv) throws IOException {
        if (mv.getId() == null) mv.setId(mv.getMappingKey() + ":" + mv.getVersion());
        IndexRequest<FieldMapping> req = IndexRequest.of(b -> b
                .index(index).id(mv.getId()).document(mv).refresh(Refresh.WaitFor));
        es.index(req);
        return mv;
    }

    public long nextVersion(String mappingKey) throws IOException {
        SearchResponse<FieldMapping> sr = es.search(SearchRequest.of(b -> b
                .index(index)
                .query(q -> q.term(t -> t.field("mappingKey.keyword").value(mappingKey)))
                .sort(s -> s.field(f -> f.field("version").order(
                        co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                .size(1)), FieldMapping.class);
        long current = 0L;
        for (Hit<FieldMapping> h : sr.hits().hits()) {
            current = Math.max(current, h.source().getVersion());
        }
        return current + 1;
    }
}
