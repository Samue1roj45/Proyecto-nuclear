package com.psicosocial.simulador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.oauth.google-client-id:}")
    private String googleClientId;

    @Value("${app.oauth.google-client-secret:}")
    private String googleClientSecret;

    @Value("${app.oauth.facebook-app-id:}")
    private String facebookAppId;

    @Value("${app.oauth.facebook-app-secret:}")
    private String facebookAppSecret;

    public OAuthProfile verifyGoogleCode(String code) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Google OAuth no está configurado (GOOGLE_CLIENT_ID)");
        }
        if (googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new RuntimeException("Google OAuth no está configurado (GOOGLE_CLIENT_SECRET)");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", "postmessage");
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", entity, String.class);
            JsonNode tokenData = objectMapper.readTree(response.getBody());
            String idToken = text(tokenData, "id_token");
            if (idToken == null) {
                throw new RuntimeException("Google no devolvió un token válido");
            }
            return verifyGoogleIdToken(idToken);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo autenticar con Google");
        }
    }

    private OAuthProfile verifyGoogleIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        JsonNode data = fetchJson(url);

        String aud = text(data, "aud");
        if (!googleClientId.equals(aud)) {
            throw new RuntimeException("Token de Google no válido para esta aplicación");
        }

        String email = text(data, "email");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google no proporcionó un correo electrónico");
        }

        return new OAuthProfile(
                text(data, "sub"),
                email.toLowerCase(),
                text(data, "name"),
                text(data, "picture")
        );
    }

    public OAuthProfile verifyFacebookToken(String accessToken) {
        if (facebookAppId == null || facebookAppId.isBlank()) {
            throw new RuntimeException("Facebook OAuth no está configurado (FACEBOOK_APP_ID)");
        }
        if (facebookAppSecret == null || facebookAppSecret.isBlank()) {
            throw new RuntimeException("Facebook OAuth no está configurado (FACEBOOK_APP_SECRET)");
        }

        String debugUrl = UriComponentsBuilder
                .fromHttpUrl("https://graph.facebook.com/debug_token")
                .queryParam("input_token", accessToken)
                .queryParam("access_token", facebookAppId + "|" + facebookAppSecret)
                .toUriString();
        JsonNode debug = fetchJson(debugUrl).path("data");

        if (!debug.path("is_valid").asBoolean(false)) {
            throw new RuntimeException("Token de Facebook no válido");
        }
        if (!facebookAppId.equals(debug.path("app_id").asText())) {
            throw new RuntimeException("Token de Facebook no pertenece a esta aplicación");
        }

        String profileUrl = UriComponentsBuilder
                .fromHttpUrl("https://graph.facebook.com/me")
                .queryParam("fields", "id,name,email,picture.type(large)")
                .queryParam("access_token", accessToken)
                .toUriString();
        JsonNode profile = fetchJson(profileUrl);

        String email = text(profile, "email");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Facebook no proporcionó un correo. Autoriza el permiso de email.");
        }

        String picture = profile.path("picture").path("data").path("url").asText(null);

        return new OAuthProfile(
                text(profile, "id"),
                email.toLowerCase(),
                text(profile, "name"),
                picture
        );
    }

    private JsonNode fetchJson(String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo verificar la cuenta social");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    public record OAuthProfile(String providerId, String email, String fullName, String avatarUrl) {}
}
