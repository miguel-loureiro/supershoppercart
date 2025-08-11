package com.supershoppercart.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    @Value("${oauth.id}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        logger.info("GoogleTokenVerifier initialized with client ID: {}", googleClientId);
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {

                GoogleIdToken.Payload payload = idToken.getPayload();
                logger.debug("Token verified successfully for user: {}", payload.getEmail());
                return payload;
            } else {

                logger.warn("Token verification failed - invalid token");
                return null;
            }
        } catch (GeneralSecurityException e) {

            logger.error("Security exception during token verification", e);
            return null;

        } catch (IOException e) {

            logger.error("IO exception during token verification", e);
            return null;

        } catch (Exception e) {

            logger.error("Unexpected exception during token verification", e);
            return null;
        }
    }
}