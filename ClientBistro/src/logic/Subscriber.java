package logic;

import java.io.Serializable;

/**
 * Represents a subscriber in the system.
 * <p>
 * This class stores subscriber details such as identifier, username,
 * contact phone number, and email address.
 * It is commonly used as a data model or Data Transfer Object (DTO).
 * </p>
 */
public class Subscriber implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the subscriber */
    private int subscriberId;

    /** The username of the subscriber */
    private String username;

    /** The subscriber's phone number */
    private String phone;

    /** The subscriber's email address */
    private String email;

    /**
     * Constructs a {@code Subscriber} object.
     * Initializes the subscriber's personal and contact details.
     *
     * @param id       the unique identifier of the subscriber
     * @param username the subscriber's username
     * @param email    the subscriber's email address
     * @param phone    the subscriber's phone number
     */
    public Subscriber(int id, String username, String email, String phone) {
        this.subscriberId = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Returns the subscriber ID.
     *
     * @return the subscriber ID
     */
    public int getSubscriberId() {
        return subscriberId;
    }

    /**
     * Sets the subscriber ID.
     *
     * @param subscriberId the subscriber ID
     */
    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    /**
     * Returns the subscriber's username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the subscriber's username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the subscriber's phone number.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the subscriber's phone number.
     *
     * @param phone the phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns the subscriber's email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the subscriber's email address.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns a string representation of the subscriber.
     *
     * @return a string representation of the subscriber
     */
    @Override
    public String toString() {
        return "Subscriber{id=" + subscriberId
                + ", username='" + username + '\''
                + ", phone='" + phone + '\''
                + ", email='" + email + '\''
                + '}';
    }
}