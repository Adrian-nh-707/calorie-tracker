package com.group19.calorietrack.controller;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import com.group19.calorietrack.services.FatSecretService;

@RestController
@RequestMapping("/api/fatsecret")
public class FatSecretApiController {

    private final FatSecretService fatSecretService;

    public FatSecretApiController(FatSecretService fatSecretService) {
        this.fatSecretService = fatSecretService;
    }

    // Search dishes

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        if (query.isBlank() || query.length() > 100 || page < 0) {
            return error(
                HttpStatus.BAD_REQUEST,
                "Invalid search query."
            );
        }

        try {
            Map<?, ?> result =
                    fatSecretService.searchRecipes(query, page);

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
            Map<?, ?> result =
                    fatSecretService.getRecipe(id);

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

    private ResponseEntity<?> apiResponse(Map<?, ?> result) {

        if (result == null) {
            return error(
                HttpStatus.BAD_GATEWAY,
                "Empty FatSecret response."
            );
        }

        if (result.containsKey("error")) {

            Object apiError = result.get("error");

            if (apiError instanceof Map<?, ?> details) {

                String code = String.valueOf(details.get("code"));

                if ("13".equals(code)) {
                    fatSecretService.clearToken();
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
            fatSecretService.clearToken();
        }

        if (status == 429) {
            return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "FatSecret API limit reached."
            );
        }

        if (status == 400 || status == 401 || status == 403) {
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