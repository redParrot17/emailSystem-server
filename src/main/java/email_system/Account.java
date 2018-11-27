package email_system;

import server.listener_references.Email;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Account {

    private String email_address;
    private byte[] password;
    private SortedSet<Email> receivedEmails;

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
        receivedEmails = new TreeSet<>();
    }

    /**
     * 
     * @return the users email address
     */
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

    	// returns a deep copy to avoid any possible tampering/loss of data
    	LinkedHashSet<Email> receivedEmailsCopy = new LinkedHashSet<>();
    	for (Email email : receivedEmails) {
			receivedEmailsCopy.add(new Email(email));
		}
    	
        return receivedEmailsCopy;
    }

    /**
     * Returns a {@link LinkedHashSet} of the last {@code count} number of emails added
     * @param count number of emails to return
     * @return
     */
    public SortedSet<Email> getReceivedEmails(int count) {
    	SortedSet<Email> copy = new TreeSet<>();
    	Iterator<Email> iter = receivedEmails.iterator();
    	for (int i = 0; i < count; i++) {
    		copy.add(new Email(iter.next()));
    	}
        return copy;
    }

    /**
     * @return the number of emails currently held withing this account
     */
    public int getTotalEmailCount() {
        return receivedEmails.size();
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
     * @param email the {@link Email} to be removed from this account
     */
    public void removeEmail(Email email) {
        receivedEmails.remove(email);
    }

    /**
     * @param uuid the unique identifier of the email you wish to retrieve
     * @return {@link Optional<Email>} of connected to the uuid
     */
    public Optional<Email> getEmailFromUUID(String uuid) {
        return receivedEmails.stream().filter(email -> email.getUUID().equals(uuid)).findFirst();
    }
}
