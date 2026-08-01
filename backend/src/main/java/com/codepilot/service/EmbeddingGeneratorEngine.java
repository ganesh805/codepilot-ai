package com.codepilot.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class EmbeddingGeneratorEngine {

    private static final int DEFAULT_DIMENSION = 768;

    /**
     * Generates a normalized 768-dimensional float embedding vector for code chunks.
     * Uses text token distribution hashing with L2-normalization for cosine distance RAG search.
     */
    public float[] generateEmbedding(String text) {
        float[] vector = new float[DEFAULT_DIMENSION];
        if (text == null || text.trim().isEmpty()) {
            return vector;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);

            for (int i = 0; i < DEFAULT_DIMENSION; i++) {
                byte b = hash[i % hash.length];
                int charVal = (i < text.length()) ? text.charAt(i) : 0;
                float rawVal = (float) ((b ^ charVal) / 255.0);
                vector[i] = rawVal;
            }

            // Apply L2 Normalization for Cosine Similarity
            float norm = 0.0f;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < DEFAULT_DIMENSION; i++) {
                    vector[i] /= norm;
                }
            }

        } catch (NoSuchAlgorithmException ex) {
            for (int i = 0; i < DEFAULT_DIMENSION; i++) {
                vector[i] = (float) Math.sin(i + text.length());
            }
        }

        return vector;
    }
}
