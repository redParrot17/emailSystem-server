package server.packets;

import server.listener_references.Email;

/**
 * A wrapper for the {@link Email} class to be sent via an {@link EncryptionPacket}
 */
public class EmailPacket extends DataPacket {

    private String author;
    private String[] recipients;
    private String subject;
    private String message;
    private boolean hasRead;

    /**
     * @param author     the author of the email
     * @param recipients the recipients of the email
     * @param subject    the email's subject line
     * @param message    the message the email contains
     */
    public EmailPacket(String author, String[] recipients, String subject, String message) {
        super();
        this.author = author;
        this.recipients = recipients;
        this.subject = subject;
        this.message = message;
        this.hasRead = false;
    }

    /**
     * Creates a deep copy of the {@code email}
     * @param email the old email to be deep copied
     */
    public EmailPacket(Email email) {
        super(email.getCreationTimestamp());
        this.author = email.getAuthor();
        this.recipients = email.getRecipients();
        this.subject = email.getSubject();
        this.message = email.getMessage();
        this.hasRead = email.hasOpened();
    }

    public String getAuthor() {
        return author;
    }

    public String[] getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasOpened() {
        return hasRead;
    }
}