package com.sportscenter.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {
    private final String clientId;
    private volatile GoogleIdTokenVerifier verifier;
    private volatile HttpTransport transport;

    public GoogleTokenVerifierImpl(@Value("${google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public GoogleIdentity verify(String credential) {
        if (clientId == null || clientId.isBlank() || credential == null || credential.isBlank()) {
            throw invalidToken();
        }
        try {
            GoogleIdToken token = getVerifier().verify(credential);
            if (token == null) throw invalidToken();
            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleIdentity(token.getPayload().getSubject(), payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified()), (String) payload.get("name"),
                    (String) payload.get("picture"));
        } catch (GeneralSecurityException | IOException | RuntimeException exception) {
            if (exception instanceof BadCredentialsException badCredentials) throw badCredentials;
            throw invalidToken();
        }
    }

    private GoogleIdTokenVerifier getVerifier() throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier current = verifier;
        if (current == null) {
            synchronized (this) {
                current = verifier;
                if (current == null) {
                    HttpTransport currentTransport = GoogleNetHttpTransport.newTrustedTransport();
                    GoogleIdTokenVerifier created = new GoogleIdTokenVerifier.Builder(
                            currentTransport, GsonFactory.getDefaultInstance())
                            .setAudience(List.of(clientId)).build();
                    transport = currentTransport;
                    verifier = current = created;
                }
            }
        }
        return current;
    }

    private BadCredentialsException invalidToken() {
        return new BadCredentialsException("Login failed");
    }
}
