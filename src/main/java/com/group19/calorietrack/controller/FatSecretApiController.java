
package com.group19.calorietrack.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/fatsecret")
public class FatSecretApiController {

    @Value("${FATSECRET_CLIENT_ID:}")
    private String clientId;

    @Value("${FATSECRET_CLIENT_SECRET:}")
    private String clientSecret;

    private final RestClient client;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public FatSecretApiController() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);

        client = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    // Obtain and reuse OAuth access token

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

    private synchronized void clearToken() {
        accessToken = null;
        expiresAt = Instant.EPOCH;
    }

    // Search dishes

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("query") String query,

            @RequestParam(
                value = "page",
                defaultValue = "0"
            ) int page) {

        if (query.isBlank() ||
                query.length() > 100 ||
                page < 0) {

            return error(
                HttpStatus.BAD_REQUEST,
                "Invalid search query."
            );
        }

        try {

            Map<?, ?> result = client.get()
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
                    headers.setBearerAuth(
                        getAccessToken()
                    )
                )
                .retrieve()
                .body(Map.class);

            return apiResponse(result);

        } catch (RestClientResponseException e) {

            return upstreamError(e);

        } catch (IllegalStateException e) {

            return error(
                HttpStatus.BAD_GATEWAY,
                "Check your FatSecret credentials."
            );

        } catch (Exception e) {

            return error(
                HttpStatus.BAD_GATEWAY,
                "Unable to connect to FatSecret."
            );
        }
    }

    // Get ingredients, cooking instructions and serving details

    @GetMapping("/recipe")
    public ResponseEntity<?> recipe(
            @RequestParam("id") String id) {

        if (!id.matches("[0-9]{1,18}")) {

            return error(
                HttpStatus.BAD_REQUEST,
                "Invalid recipe ID."
            );
        }

        try {

            Map<?, ?> result = client.get()
                .uri(
                    "https://platform.fatsecret.com" +
                    "/rest/recipe/v2" +
                    "?recipe_id={id}&format=json",
                    id
                )
                .headers(headers ->
                    headers.setBearerAuth(
                        getAccessToken()
                    )
                )
                .retrieve()
                .body(Map.class);

            return apiResponse(result);

        } catch (RestClientResponseException e) {

            return upstreamError(e);

        } catch (Exception e) {

            return error(
                HttpStatus.BAD_GATEWAY,
                "Unable to load recipe details."
            );
        }
    }

    // Handle FatSecret responses

    private ResponseEntity<?> apiResponse(
            Map<?, ?> result) {

        if (result == null) {

            return error(
                HttpStatus.BAD_GATEWAY,
                "Empty FatSecret response."
            );
        }

        if (result.containsKey("error")) {

            Object apiError = result.get("error");

            if (apiError instanceof Map<?, ?> details) {

                String code = String.valueOf(
                    details.get("code")
                );

                if ("13".equals(code)) {
                    clearToken();
                }
            }

            return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .cacheControl(CacheControl.noStore())
                .body(result);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    private ResponseEntity<?> upstreamError(
            RestClientResponseException e) {

        int status = e.getStatusCode().value();

        if (status == 401) {
            clearToken();
        }

        if (status == 429) {

            return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "FatSecret API limit reached."
            );
        }

        if (status == 400 ||
                status == 401 ||
                status == 403) {

            return error(
                HttpStatus.BAD_GATEWAY,
                "FatSecret authorization failed. " +
                "Check credentials, scope and IP whitelist."
            );
        }

        return error(
            HttpStatus.BAD_GATEWAY,
            "FatSecret request failed: HTTP " + status
        );
    }

    private ResponseEntity<?> error(
            HttpStatus status,
            String message) {

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("error", message));
    }
}
