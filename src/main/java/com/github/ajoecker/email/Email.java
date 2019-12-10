package com.github.ajoecker.email;

/**
 * {@link Email} is an abstraction of a sent email
 */
public final class Email {
    private String id;
    private String content;

    public Email(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String id() {
        return id;
    }

    public String content() {
        return content;
    }

    static Email of(String id, String content) {
        return new Email(id, content);
    }
}
