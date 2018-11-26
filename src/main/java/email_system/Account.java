package email_system;

import server.listener_references.Email;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;

public class Account {

    private String email_address;
    private byte[] password;
    private LinkedHashSet<Email> receivedEmails;

    /**
     * @param email_address unique email address to be associated with this account
     * @param password      character array of the password to be associated with this account
     */
    public Account(String email_address, char[] password) {
        this.email_address = email_address;
        try {
            byte[] pass = new String(password).getBytes("UTF-8");
            MessageDigest sha3 = MessageDigest.getInstance("SHA3-512");
            for (byte b : pass) { sha3.update(b); }
            this.password = sha3.digest();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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
        try {
            byte[] pass = new String(password).getBytes("UTF-8");
            MessageDigest sha3 = MessageDigest.getInstance("SHA3-512");
            for (byte b : pass) { sha3.update(b); }
            return Arrays.equals(this.password, sha3.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a {@link LinkedHashSet} of all the emails this account holds
     * @return
     */
    public LinkedHashSet<Email> getAllReceivedEmails() {

        //TODO: return all the emails. Should we return the actual ones or a deep copy?
        return receivedEmails;
    }

    /**
     * Returns a {@link LinkedHashSet} of the last {@code count} number of emails added
     * @param count number of emails to return
     * @return
     */
    public LinkedHashSet<Email> getReceivedEmails(int count) {
    	// I don't think I made this a deep copy but I tried
    	
    	LinkedHashSet<Email> copy = new LinkedHashSet<>();
    	Iterator<Email> iter = receivedEmails.iterator();
    	for (int i = 0; i < count; i++) {
    		copy.add(iter.next());
    	}
        //TODO: get a collection of the last "count" emails added to the list
        return copy;
    }

    /**
     * Adds a new email to the internal list of emails this account holds
     * @param email the {@link Email} to be added to this account
     */
    public void addEmail(Email email) {
    	receivedEmails.add(email);
    }

    /**
     * Removes the specified email from the list
     * @param email
     */
    public void removeEmail(Email email) {
        receivedEmails.remove(email);
    }

    public Optional<Email> getEmailFromUUID(String uuid) {
        return receivedEmails.stream().filter(email -> email.getUUID().equals(uuid)).findFirst();
    }
}
