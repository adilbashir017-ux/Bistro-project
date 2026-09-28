package logic;

import java.io.Serializable;

/**
 * Represents a staff member in the system.
 * <p>
 * This class stores basic staff information such as the staff identifier,
 * username, and role. It is typically used to represent employees such as
 * managers or customer service representatives.
 * </p>
 */
public class Staff implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the staff member */
    private int staffId;

    /** The username of the staff member */
    private String username;

    /** The role of the staff member (e.g., MANAGER, REPRESENTATIVE) */
    private String role;

    /**
     * Constructs a {@code Staff} object.
     * Initializes the staff member's details.
     *
     * @param staffId  the unique identifier of the staff member
     * @param username the username of the staff member
     * @param role     the role of the staff member
     */
    public Staff(int staffId, String username, String role) {
        this.staffId = staffId;
        this.username = username;
        this.role = role;
    }

    /**
     * Returns the staff member's ID.
     *
     * @return the staff ID
     */
    public int getStaffId() {
        return staffId;
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
     * Returns the staff member's role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }
}