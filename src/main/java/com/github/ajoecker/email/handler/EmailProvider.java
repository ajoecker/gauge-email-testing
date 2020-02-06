package com.github.ajoecker.email.handler;

import com.github.ajoecker.email.EmailHandler;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class EmailProvider {
    private String application;
    private Path token;

    private EmailProvider() {
    }

    public static EmailProvider newProvider() {
        return new EmailProvider();
    }

    public EmailProvider application(String application) {
        this.application = application;
        return this;
    }

    public EmailProvider token(Path token) {
        this.token = token;
        return this;
    }

    public EmailProvider token(String token) {
        return token(Paths.get(token));
    }

    public EmailHandler done() {
        return new GMailHandler(token, application);
    }
}
