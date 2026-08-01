package com.codepilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class QdrantVectorConfig {

    @Value("${app.qdrant.host:localhost}")
    private String host;

    @Value("${app.qdrant.port:6334}")
    private int port;

    @Value("${app.qdrant.collection-name:codepilot_code_chunks}")
    private String collectionName;

    @Value("${app.qdrant.vector-dimension:768}")
    private int vectorDimension;
}
