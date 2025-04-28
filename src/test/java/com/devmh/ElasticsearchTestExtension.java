package com.devmh;

import org.junit.jupiter.api.extension.*;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ElasticsearchTestExtension implements BeforeAllCallback, AfterEachCallback {

    private static final ElasticsearchContainer container =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.4")
                    .withEnv("xpack.security.enabled", "false"); // simplify setup

    private static boolean started = false;

    private final List<Class<?>> domainClasses;

    public ElasticsearchTestExtension(Class<?>... entityClasses) {
        this.domainClasses = Arrays.asList(entityClasses);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            container.start();
            started = true;
            System.setProperty("spring.elasticsearch.uris", container.getHttpHostAddress());
        }
        clearIndices(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        clearIndices(context);
    }

    private void clearIndices(ExtensionContext context) {
        ApplicationContext appCtx = SpringExtension.getApplicationContext(context);
        ElasticsearchOperations ops = appCtx.getBean(ElasticsearchOperations.class);
        for (Class<?> domainClass : domainClasses) {
            IndexOperations indexOps = ops.indexOps(domainClass);
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.create();
            indexOps.putMapping();
        }
    }

    public static String getElasticsearchUri() {
        return container.getHttpHostAddress();
    }

    public static Set<Class<?>> scanEntities(Class<?>... types) {
        return Arrays.stream(types)
                .filter(c -> c.isAnnotationPresent(Document.class))
                .collect(Collectors.toSet());
    }
}

