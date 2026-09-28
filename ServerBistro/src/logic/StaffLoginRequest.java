package logic;

import java.io.Serializable;

/**
 * Represents a login request for a staff member.
 * <p>
 * This class is used to encapsulate the credentials required for
 * staff authentication, including username and password.
 * </p>
 */
public class StaffLoginRequest implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The username of the staff member */
    private String username;

    /** The password of the staff member */
    private String password;

    /**
     * Constructs a {@code StaffLoginRequest} object.
     * Initializes the staff login credentials.
     *
     * @param username the staff member's username
     * @param password the staff member's password
     */
    public StaffLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the staff member's username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the staff member's password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}