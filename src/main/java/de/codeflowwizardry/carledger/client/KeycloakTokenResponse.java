package de.codeflowwizardry.carledger.client;

public record KeycloakTokenResponse(String access_token, String refresh_token, String token_type, long expires_in) {
}
