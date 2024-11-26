package org.geant.maatjavauser;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class Authentication {

    private final String authUrl;
    private final String tokenUrl;
    private final String maatResourceUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String username;
    private final String password;

    public Authentication(String authUrl, String tokenUrl, String maatResourceUrl, String clientId, String clientSecret, String redirectUri, String username, String password) {
        this.authUrl = authUrl;
        this.tokenUrl = tokenUrl;
        this.maatResourceUrl = maatResourceUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.username = username;
        this.password = password;
    }

    private static final HttpClient client;

    static {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public void getToken() throws IOException, InterruptedException {
        HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(URI.create(authUrl + "?response_type=code&client_id=" + clientId + "&redirect_uri=" + redirectUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        HttpResponse<String> authResponse = client.send(authRequest, HttpResponse.BodyHandlers.ofString());

        if (authResponse.statusCode() == 200 && authResponse.body().contains("kc-page-title")) {
            String formHtml = authResponse.body();
            Pattern actionPattern = Pattern.compile("action=\"(.*?)\"");
            Matcher matcher = actionPattern.matcher(formHtml);
            String loginUrl;

            if (matcher.find()) {
                loginUrl = matcher.group(1);
            } else {
                throw new IllegalStateException("URL not found in the form's action attribute.");
            }

            Map<Object, Object> formData = Map.of(
                    "username", username,
                    "password", password
            );
            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .uri(URI.create(loginUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(buildFormDataFromMap(formData))
                    .build();

            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

            if (loginResponse.headers().firstValue("Location").isPresent()) {
                String location = loginResponse.headers().firstValue("Location").get();
                String authorizationCode = location.substring(location.indexOf("code=") + 5);

                System.out.println("Code: " + authorizationCode);

                HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(buildFormDataFromMap(Map.of(
                                "grant_type", "authorization_code",
                                "code", authorizationCode,
                                "redirect_uri", redirectUri,
                                "client_id", clientId,
                                "client_secret", clientSecret
                        )))
                        .build();

                HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

                if (tokenResponse.statusCode() == 200) {
                    JSONObject json = new JSONObject(tokenResponse.body());
                    String jsonToken = json.getString("access_token");
                    System.out.println("Token -> " + jsonToken);
                    getResourceWithToken(jsonToken);
                } else {
                    System.out.println("Failed to get token");
                }
            } else {
                System.out.println("Failed to get code");
            }
        } else {
            System.out.println("Failed to get login page");
        }
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
                .uri(URI.create(maatResourceUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> maat_response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response code from Maat: " + maat_response.statusCode());
        System.out.println("Response from Maat: " + maat_response.body());
    }
}
