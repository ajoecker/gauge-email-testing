package com.github.ajoecker.email.handler;

import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.Paths.get;
import static org.assertj.core.api.Assertions.assertThat;

public class GMailHandlerTest {
    @Test
    public void password_reset_link_can_be_parsed() throws URISyntaxException, IOException {
        String text = new String(Files.readAllBytes(get(GMailHandlerTest.class.getResource("/password-zuruecksetzen").toURI())));
        GMailHandler gMailHandler = new GMailHandler(Paths.get(System.getProperty("user.dir", "tokens"))) {
            @Override
            protected String getRawEmail(Gmail gmail, String messageId) {
                return text;
            }
        };
        String link = gMailHandler.getPasswordForgottenLink("something");
        assertThat(link).isEqualTo("https://stage.nexible.de/kundenbereich/passwort-zuruecksetzen/iC_gkdMsrL3gwR3bdMsW");
    }


    @Test
    public void account_verification_can_be_parsed() throws URISyntaxException, IOException {
        String text = new String(Files.readAllBytes(get(GMailHandlerTest.class.getResource("/zugangsdaten-bestaetigen").toURI())));
        GMailHandler gMailHandler = new GMailHandler(Paths.get(System.getProperty("user.dir", "tokens"))) {
            @Override
            protected String getRawEmail(Gmail gmail, String messageId) {
                return text;
            }
        };
        String link = gMailHandler.getAccountVerificationLink("something");
        assertThat(link).isEqualTo("https://stage.nexible.de/kundenbereich/email-verifizieren/hmNiNeRztdsTVsGjVDVc");
    }
}
