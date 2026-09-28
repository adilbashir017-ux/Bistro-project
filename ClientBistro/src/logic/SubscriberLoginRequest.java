package logic;

import java.io.Serializable;

/**
 * Represents a login request for a subscriber.
 * <p>
 * This class is used to encapsulate the information required
 * to authenticate a subscriber, including subscriber ID and name.
 * </p>
 */
public class SubscriberLoginRequest implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The subscriber's identifier */
    private String subscriberId;

    /** The subscriber's name */
    private String subscriberName;

    /**
     * Constructs a {@code SubscriberLoginRequest} object.
     * Initializes the subscriber login details.
     *
     * @param id   the subscriber's identifier
     * @param name the subscriber's name
     */
    public SubscriberLoginRequest(String id, String name) {
        this.subscriberId = id;
        this.subscriberName = name;
    }

    /**
     * Returns the subscriber ID.
     *
     * @return the subscriber ID
     */
    public String getSubscriberId() {
        return subscriberId;
    }

    /**
     * Returns the subscriber's name.
     *
     * @return the subscriber's name
     */
    public String getSubscriberName() {
        return subscriberName;
    }
}