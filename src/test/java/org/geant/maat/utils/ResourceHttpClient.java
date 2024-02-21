package org.geant.maat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceHttpClient extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;

    public ResourceHttpClient() {
        client = defaultClientBuilder().build();
    }

    public HttpResponse<String> sendGet(String url) {
        try {
            var request = defaultHttpRequestBuilder()
                    .uri(new URI(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode sendGetAndParse(String url) {
        var response = sendGet(url);
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        try {
            return mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> sendPost(String url, String json) {
        HttpClient client = defaultClientBuilder().build();
        try {
            var request = defaultHttpRequestBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(new URI(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode sendPostAndParse(String url, String json) {
        var response = sendPost(url, json);
        assertEquals(HttpStatus.CREATED.value(), response.statusCode());
        try {
            return mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> sendDelete(String url) {
        try {
            var request = defaultHttpRequestBuilder().DELETE()
                    .uri(new URI(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> sendPatch(String url, String json) {
        try {
            var request = defaultHttpRequestBuilder()
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "merge-patch/json")
                    .uri(new URI(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode sendPatchAndParse(String url, String json) {
        var response = sendPatch(url, json);
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        try {
            return mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClient.Builder defaultClientBuilder() {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000));
    }

    private HttpRequest.Builder defaultHttpRequestBuilder() {
        return HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofMillis(1000));
    }

    private HttpRequest.Builder defaultPostBuilder(String json) {
        return defaultHttpRequestBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json));
    }
}
