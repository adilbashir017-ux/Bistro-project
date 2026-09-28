package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The {@code ClientUI} class is the main JavaFX application class
 * for the client-side interface of the system.
 * <p>
 * It launches the GUI, initializes the {@link ClientController},
 * and manages the connection lifecycle to the server.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Create and connect a {@link ClientController} to the server.</li>
 *     <li>Load the initial GUI interface for role selection.</li>
 *     <li>Handle proper disconnection when the application closes.</li>
 * </ul>
 *
 * @author Mohamad
 * @version 1.0
 */
public class ClientUI extends Application {

    /** The main client controller used by this application. */
    public static ClientController chat;

    /**
     * The main entry point for JavaFX applications.
     * Initializes the client and loads the GUI.
     *
     * @param primaryStage the primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Create client controller
            String serverHost = System.getenv().getOrDefault("BISTRO_SERVER_HOST", "localhost");
            int serverPort = Integer.parseInt(
                    System.getenv().getOrDefault("BISTRO_SERVER_PORT", "5555")
            );
            chat = new ClientController(serverHost, serverPort);

            // Force connection
            chat.openConnection();

        } catch (Exception e) {
            System.out.println("❌ Server is OFF, cannot connect");
            showServerError();
            return;
        }

        try {
            // Load main interface FXML
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Interface.fxml"));

            primaryStage.setTitle("Select Interface");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            // Proper disconnect on window close
            primaryStage.setOnCloseRequest(event -> {
                try {
                    if (chat != null) {
                        chat.closeConnection();   // FORCE disconnect
                        System.out.println("[CLIENT] Connection closed");
                    }
                } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Problem loading RoleSelection.fxml");
        }
    }

    /**
     * Called by JavaFX when the application stops.
     * Ensures the client connection is closed properly.
     */
    @Override
    public void stop() {
        try {
            if (chat != null) {
                chat.closeConnection();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Displays an error alert to the user if the server is unavailable.
     */
    private void showServerError() {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Connection Failed");
        alert.setHeaderText("Server Not Available");
        alert.setContentText("Please start the server and try again.");
        alert.showAndWait();
    }

    /**
     * The main method that launches the JavaFX application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}