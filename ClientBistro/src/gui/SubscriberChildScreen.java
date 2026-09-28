package gui;

import client.ClientController;
import logic.Subscriber;

/**
 * Interface for screens that are children of the subscriber homepage.
 * <p>
 * Any screen implementing this interface must be able to receive the subscriber
 * information and a reference to the client controller for communication with the server.
 * </p>
 */
public interface SubscriberChildScreen {

    /**
     * Sets the client controller for this screen.
     * <p>
     * The client controller is used to communicate with the server and send requests
     * related to this subscriber screen.
     * </p>
     *
     * @param client the {@link ClientController} instance for server communication
     */
    void setClient(ClientController client);

    /**
     * Sets the subscriber information for this screen.
     * <p>
     * Implementing classes should store this subscriber object and use it
     * to display personalized information or perform actions specific to the subscriber.
     * </p>
     *
     * @param subscriber the {@link Subscriber} whose data is being displayed or used
     */
    void setSubscriber(Subscriber subscriber);
}