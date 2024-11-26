package org.geant.maatjavaclient;

import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;



public class Authentication {

    private final String tokenUrl;
    private final String resourceUrl;
    private final String clientId;
    private final String clientSecret;

    public Authentication(String tokenUrl, String resourceUrl, String clientId, String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.resourceUrl = resourceUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private static final HttpClient client = HttpClient.newBuilder().build();

    public void getToken() throws IOException, InterruptedException {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", authHeader)
                .POST(buildFormDataFromMap(Map.of(
                        "grant_type", "client_credentials")))
                .build();

        HttpResponse<String> response = client.send(authRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from Keycloak: " + response.body());

        JSONObject json = new JSONObject(response.body());
        String jsonToken = json.getString("access_token");
        System.out.println("Token -> " + jsonToken);
        getResourceWithToken(jsonToken);
    }

    public void getResourceWithToken(String token) throws IOException, InterruptedException {
        X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        var client = HttpClient.newBuilder().sslContext(sslContext).build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resourceUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> maat_response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response code from Maat: " + maat_response.statusCode());
        System.out.println("Response from Maat: " + maat_response.body());
    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

}

