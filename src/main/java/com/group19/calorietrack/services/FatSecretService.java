package com.group19.calorietrack.services;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class FatSecretService {

    @Value("${FATSECRET_CLIENT_ID:}")
    private String clientId;

    @Value("${FATSECRET_CLIENT_SECRET:}")
    private String clientSecret;

    private final RestClient client;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public FatSecretService() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);

        client = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public Map<?, ?> searchRecipes(String query, int page) {
        return client.get()
                .uri(
                    "https://platform.fatsecret.com" +
                    "/rest/recipes/search/v3" +
                    "?search_expression={query}" +
                    "&page_number={page}" +
                    "&max_results=12" +
                    "&must_have_images=true" +
                    "&format=json",
                    query.trim(),
                    page
                )
                .headers(headers ->
                    headers.setBearerAuth(getAccessToken())
                )
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> getRecipe(String id) {
        return client.get()
                .uri(
                    "https://platform.fatsecret.com" +
                    "/rest/recipe/v2" +
                    "?recipe_id={id}&format=json",
                    id
                )
                .headers(headers ->
                    headers.setBearerAuth(getAccessToken())
                )
                .retrieve()
                .body(Map.class);
    }

    private synchronized String getAccessToken() {

        if (accessToken != null &&
                Instant.now().isBefore(expiresAt)) {
            return accessToken;
        }

        if (clientId.isBlank() ||
                clientSecret.isBlank()) {
            throw new IllegalStateException(
                "FatSecret credentials are missing."
            );
        }

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("scope", "basic");

        Map<?, ?> response = client.post()
                .uri("https://oauth.fatsecret.com/connect/token")
                .headers(headers ->
                    headers.setBasicAuth(
                        clientId,
                        clientSecret
                    )
                )
                .contentType(
                    MediaType.APPLICATION_FORM_URLENCODED
                )
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null ||
                response.get("access_token") == null ||
                response.get("expires_in") == null) {
            throw new IllegalStateException(
                "Unable to obtain FatSecret token."
            );
        }

        accessToken = response
                .get("access_token")
                .toString();

        long expiresIn = Long.parseLong(
            response.get("expires_in").toString()
        );

        expiresAt = Instant.now().plusSeconds(
            Math.max(0, expiresIn - 60)
        );

        return accessToken;
    }

    public synchronized void clearToken() {
        accessToken = null;
        expiresAt = Instant.EPOCH;
    }
}