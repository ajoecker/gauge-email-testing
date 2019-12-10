package com.github.ajoecker.email.handler;

import com.github.ajoecker.email.EmailHandler;
import org.tinylog.Logger;

public enum EmailProvider {
    GMAIL {
        @Override
        public EmailHandler handler() {
            return new GMailHandler();
        }
    };
    public abstract EmailHandler handler();

    public static EmailHandler from(String emailHandler) {
        Logger.info("using {} as email handler", emailHandler);
        if (emailHandler == null || emailHandler.equals("")) {
            Logger.warn("no email provider configured, using default 'Gmail'");
            return GMAIL.handler();
        }
        return EmailProvider.valueOf(emailHandler.toUpperCase()).handler();
    }
}
