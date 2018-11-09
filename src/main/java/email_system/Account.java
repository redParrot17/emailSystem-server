package email_system;

import server.listener_references.Email;

import java.util.LinkedHashSet;

public class Account {

    private String email_address;
    private final char[] password;
    private LinkedHashSet<Email> receivedEmails;

    public Account(String email_address, char[] password) {
        this.email_address = email_address;
        //TODO: do something to password so that you don't store the real one
        this.password = password; // store the modified one
        receivedEmails = new LinkedHashSet<>();
    }

    public String getEmail_address() {
        return email_address;
    }

    /**
     * Compares the parameter {@code password} with the one stored in the account
     * @param password password to check
     * @return true if the {@code password} matches the one connected to this account
     */
    public boolean checkPassword(char[] password) {
        //TODO: do the same thing to the parameter that you did in the constructor
        return this.password == password; // compare this.password to the modified one
    }

    /**
     * Returns a {@link LinkedHashSet} of all the emails this account holds
     * @return
     */
    public LinkedHashSet<Email> getAllReceivedEmails() {
        //TODO: return all the emails. Should we return the actual ones or a deep copy?
        return null;
    }

    /**
     * Returns a {@link LinkedHashSet} of the last {@code count} number of emails added
     * @param count number of emails to return
     * @return
     */
    public LinkedHashSet<Email> getReceivedEmails(int count) {
        //TODO: get a collection of the last "count" emails added to the list
        return null;
    }

    /**
     * Adds a new email to the internal list of emails this account holds
     * @param email the {@link Email} to be added to this account
     */
    public void addEmail(Email email) {
        //TODO: add the email to the list
    }

    /**
     * Removes the specified email from the list
     * @param email
     */
    public void removeEmail(Email email) {
        //TODO: remove the email from the list
    }

}
