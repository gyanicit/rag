package com.icit.rag.config;

import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModelName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Spring configuration for the application's embedding and retrieval pieces.
 *
 * <p>At startup, Spring calls each {@code @Bean} method and stores the returned object
 * in the application context. Other beans can then request these objects by type (or
 * by qualifier when more than one bean has the same type).</p>
 *
 * <p>The RAG flow configured here is:</p>
 * <ol>
 *   <li>Create one Mistral embedding model.</li>
 *   <li>Connect two pgvector stores to separate knowledge tables.</li>
 *   <li>Create one retriever per store, both using the same embedding model.</li>
 *   <li>Route each query to both retrievers and combine the retrieved content.</li>
 * </ol>
 */
@Configuration
public class EmbeddingModelConfig {

    /**
     * Spring resolves this placeholder from application configuration or an
     * environment/property override. Keep the actual API key out of source control;
     * in deployment, supply it through a secret or environment variable.
     */
    @Value("${chat-model.api-key}")
    private String apiKey;

    /**
     * Defines the embedding model used to convert document text and user queries
     * into vectors. Using the same model for ingestion and search is essential:
     * their vectors must live in the same embedding space and have the same length.
     *
     * @return the Mistral embedding model managed as a Spring bean
     */
    @Bean
    public MistralAiEmbeddingModel mistralAiEmbeddingModel(){
        return MistralAiEmbeddingModel.builder()
                // The API key is injected from configuration above, rather than
                // being embedded in this Java source file.
                .apiKey(apiKey)
                // Select Mistral's text embedding model. This model creates the
                // numeric vectors stored in pgvector and compared during retrieval.
                .modelName(MistralAiEmbeddingModelName.MISTRAL_EMBED)
                .build();
    }
}
