package com.devmh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elastic.host}") private String host;
    @Value("${elastic.port}") private int port;
    @Value("${elastic.user}") private String username;
    @Value("${elastic.apiKey}") private String apiKey;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(host + ":" + port)
                //.usingSsl()
                .withBasicAuth(username, apiKey)
                .build();
    }
}
