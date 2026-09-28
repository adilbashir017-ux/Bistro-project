package client;

import java.io.IOException;
import common.ChatIF;

/**
 * The {@code ClientController} class acts as a controller for the {@link ChatClient}.
 * <p>
 * It implements the {@link ChatIF} interface, allowing it to display messages
 * and interact with the client-side UI or console.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Manages the lifecycle of a {@link ChatClient} connection to the server.</li>
 *     <li>Sends messages from the client UI to the server.</li>
 *     <li>Receives messages from the server via {@link ChatClient}.</li>
 *     <li>Provides console or GUI interaction hooks through {@link ChatIF}.</li>
 * </ul>
 * <p>
 * This class is useful for both GUI and console-based client applications.
 *
 * @author Mohamad
 * @version 1.0
 */
public class ClientController implements ChatIF {

    /** Default server port, can be set externally. */
    public static int DEFAULT_PORT;

    /** The ChatClient instance used for communication with the server. */
    private ChatClient client;

    // ---------------- CONSTRUCTOR ----------------

    /**
     * Constructs a new ClientController and connects to the server at the specified host and port.
     * Opens the connection immediately.
     *
     * @param host the server host name.
     * @param port the server port number.
     */
    public ClientController(String host, int port) {
        try {
            client = new ChatClient(host, port, this);
            client.openConnection();   // 🔥 establish initial connection
            System.out.println("Client connected to server.");
        } catch (Exception e) {
            System.out.println("Error: Can't setup connection!");
            e.printStackTrace();
        }
    }

    // ---------------- SEND MESSAGE ----------------

    /**
     * Sends a message to the server.
     * If the connection is not open, it forces the creation of a socket.
     *
     * @param msg the message to send.
     */
    public void sendToServer(Object msg) {
        try {
            client.openConnection();   // 🔥 ensure socket is open
            client.sendToServer(msg);
        } catch (Exception e) {
            System.out.println("Failed sending message to server.");
            e.printStackTrace();
        }
    }

    // ---------------- DISCONNECT ----------------

    /**
     * Disconnects the client from the server gracefully.
     */
    public void disconnect() {
        try {
            if (client != null) {
                client.closeConnection();
                System.out.println("Client disconnected from server.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- CHAT INTERFACE ----------------

    /**
     * Displays a message to the client UI or console.
     * Implements {@link ChatIF#display(String)}.
     *
     * @param message the message to display.
     */
    @Override
    public void display(String message) {
        System.out.println("> " + message);
    }

    /**
     * Accepts a string input from the console or UI and sends it to the server.
     *
     * @param str the message entered by the user.
     */
    public void accept(String str) {
        client.handleMessageFromClientUI(str);
    }

    // ---------------- CONNECTION HELPERS ----------------

    /**
     * Opens the client connection explicitly.
     *
     * @throws Exception if opening the connection fails.
     */
    public void openConnection() throws Exception {
        client.openConnection();
    }

    /**
     * Closes the client connection explicitly.
     *
     * @throws Exception if closing the connection fails.
     */
    public void closeConnection() throws Exception {
        client.closeConnection();
    }
}