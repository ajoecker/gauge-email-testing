package com.github.ajoecker.email;

import org.tinylog.Logger;

import java.util.List;

/**
 * {@link EmailHandler} provides an interface to an email provider, to access emails and procees them for testing
 */
public interface EmailHandler {
    /**
     * Returns a {@link List} of {@link String}s that match the given query.
     *
     * @param query query to search for {@link Email}s
     * @return found matches or empty list. The list contains only the message ids, no content.
     * To retrieve the content see {@link #getMessage(String)}
     */
    List<String> getMessages(String query);

    /**
     * Deletes a message based on the message id
     *
     * @param messageId message to delete
     * @return whether or not deletion was successful
     */
    boolean delete(String messageId);

    /**
     * Marks a message as read
     *
     * @param messageId message to be marked
     * @return whether marking was successful
     */
    boolean markRead(String messageId);

    /**
     * Returns the link of the given email (identified by it's id), which text contains the given linkSubText.
     * <p>
     * E.g
     *
     * <code>https://someurl.com/foo/bar/T5JVydhMEqfTzshTtzWU</code>
     * <p>
     * will be returned for linkSubText, such as
     * <ol>
     * <li>foo</li>
     * <li>foo/bar</li>
     * <li>bar</li>
     * <li>MeqfTz</li>
     * <li>....</li>
     * </ol>
     * <p>
     * If more than one links matches the subtext, the first is returned.
     *
     * @param messageId   the id of the email
     * @param linkSubText the text the link must contains
     * @return the link or an empty string if none was found
     */
    String getLinkFromEmail(String messageId, String linkSubText);

    /**
     * Returns the link to reset one's password.
     *
     * @param messageId the email to get the link of
     * @return the password reset link
     */
    String getPasswordForgottenLink(String messageId);

    /**
     * Returns the link to verify one's account.
     *
     * @param messageId the email to get the link of
     * @return the account verification link
     */
    String getAccountVerificationLink(String messageId);

    /**
     * Query the handler with the given query and checks whether there is exactly one message for that query found.
     * <p>
     * If no message or more than one, a {@link IllegalStateException} is thrown
     *
     * @param query the query to find the message
     * @return the email id
     * @throws IllegalStateException if no message or multiple messages have been found for the given query
     */
    default String ensureOneMessage(String query) {
        List<String> messages = getMessages(query);
        Logger.info("{} got {} messages", query, messages.size());
        if (messages.size() != 1) {
            throw new IllegalStateException("at most one message shall be found for " + query + ", but was " + messages.size());
        }
        String currentEmailId = messages.iterator().next();
        Logger.info("current person id: {}", currentEmailId);
        if (markRead(currentEmailId)) {
            Logger.info("email {} marked as read", currentEmailId);
        }
        return currentEmailId;
    }

    /**
     * Returns the message of the given id with content
     *
     * @param messageId the email id
     * @return the complete email
     */
    Email getMessage(String messageId);
}
