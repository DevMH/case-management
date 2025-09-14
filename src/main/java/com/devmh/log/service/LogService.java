package com.devmh.log.service;

import com.devmh.log.es.ElasticFieldDescriptorProvider;
import com.devmh.log.es.ElasticLogRepository;
import com.devmh.log.model.LogEntry;
import com.devmh.log.model.FieldDescriptorProvider;
import com.devmh.log.model.PatchFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class LogService {

    private final ObjectMapper mapper;
    private final ElasticLogRepository logRepo;

    public List<String> translate(JsonNode patchArray, FieldDescriptorProvider provider) {
        return new PatchFormatter(provider).format(patchArray);
    }

    public List<String> translateWithElasticMapping(JsonNode patchArray,
                                                    ElasticFieldDescriptorProvider provider) {
        return new PatchFormatter(provider).format(patchArray);
    }

    public String logOperations(String user, String mappingKey, long mappingVersion, JsonNode patchArray) throws IOException {
        LogEntry e = new LogEntry();
        e.setUser(user);
        e.setMappingKey(mappingKey);
        e.setMappingVersion(mappingVersion);
        e.setPatchOps(patchArray);
        return logRepo.save(e).getId();
    }

    /** Load a saved log entry by id and translate it using the mapping version recorded in the log. */
    public List<String> translateLogEntry(String logId, ElasticsearchClient es, String mappingIndex) throws IOException {
        LogEntry e = logRepo.findById(logId).orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        ElasticFieldDescriptorProvider provider = new ElasticFieldDescriptorProvider(es, mappingIndex, e.getMappingKey(), e.getMappingVersion());
        return new PatchFormatter(provider).format(e.getPatchOps());
    }

}
