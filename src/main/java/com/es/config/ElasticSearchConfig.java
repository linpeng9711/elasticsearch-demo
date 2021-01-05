package com.es.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ES配置类,给容器中注入一个RestHighLevelClient
 */
@Configuration
public class ElasticSearchConfig {
    @Value("${elasticsearch.hostname}")
    private String hostName;
    @Value("${elasticsearch.port}")
    private int port;

    public static final RequestOptions COMMON_OPTIONS;

    /**
     * 统一设置请求项
     */
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        COMMON_OPTIONS = builder.build();
    }

    /**
     * 初始化ES客户端
     */
    @Bean(name = "restHighLevelClient")
    public RestHighLevelClient restHighLevelClient() {

        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(hostName, port, "http"))
        );
    }
}
