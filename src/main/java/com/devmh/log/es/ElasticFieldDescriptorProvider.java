package com.devmh.log.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.devmh.log.model.FieldDescriptor;
import com.devmh.log.model.FieldDescriptorProvider;

import java.io.IOException;
import java.util.Map;

public class ElasticFieldDescriptorProvider implements FieldDescriptorProvider {
    private final ElasticsearchClient es;
    private final String index;
    private final String mappingKey;
    private final long version;

    public ElasticFieldDescriptorProvider(ElasticsearchClient es, String index, String mappingKey, long version) {
        this.es = es;
        this.index = index;
        this.mappingKey = mappingKey;
        this.version = version;
    }

    @Override
    public Map<String, FieldDescriptor> getFieldDescriptors() {
        try {
            String id = mappingKey + ":" + version;
            GetResponse<FieldMappingVersion> resp =
                    es.get(GetRequest.of(b -> b.index(index).id(id)), FieldMappingVersion.class);
            if (!resp.found()) {
                throw new IllegalStateException("Mapping not found: " + id);
            }
            return resp.source().getDescriptors();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
