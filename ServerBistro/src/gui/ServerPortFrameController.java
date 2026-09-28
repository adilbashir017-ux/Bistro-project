package gui;

import java.net.URL;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Server.ServerUI;

/**
 * Controller class for the Server Port Frame GUI.
 * <p>
 * This class handles user interactions in the server port selection window,
 * including starting the server on a specified port and navigating to the main server GUI.
 * </p>
 */
public class ServerPortFrameController {

    /**
     * Temporary string variable (used internally).
     */
    String temp = "";

    /**
     * Exit button in the GUI.
     */
    @FXML
    private Button btnExit = null;

    /**
     * Done button in the GUI.
     */
    @FXML
    private Button btnDone = null;

    /**
     * Label to display list information.
     */
    @FXML
    private Label lbllist;

    /**
     * Text field for entering the port number.
     */
    @FXML
    private TextField portxt;

    /**
     * Observable list of strings (used internally).
     */
    ObservableList<String> list;

    /**
     * Returns the port entered by the user in the text field.
     *
     * @return the port number as a string
     */
    private String getport() {
        return portxt.getText();
    }

    /**
     * Handles the "Done" button action.
     * <p>
     * Loads the main server GUI and starts the server on the specified port.
     * If the port field is empty, a message is printed to the console.
     * </p>
     *
     * @param event the {@link ActionEvent} triggered by the button
     * @throws Exception if an error occurs while loading the GUI or starting the server
     */
    public void Done(ActionEvent event) throws Exception {
        String p = getport();

        if (p.trim().isEmpty()) {
            System.out.println("You must enter a port number");
        } else {
            // ===== FIRST load the GUI =====
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerGUI.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Server GUI");
            stage.show();

            // we MUST get controller to activate redirect
            ServerGUI controller = loader.getController();

            // ===== NOW START SERVER =====
            ServerUI.runServer(p);
        }
    }

    /**
     * Starts the server port frame GUI.
     *
     * @param primaryStage the primary {@link Stage} of the application
     * @throws Exception if an error occurs while loading the FXML
     */
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));

        Scene scene = new Scene(root);
        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Handles the "Exit" button action.
     * <p>
     * Prints a message to the console and exits the server application.
     * </p>
     *
     * @param event the {@link ActionEvent} triggered by the button
     * @throws Exception not expected in normal operation
     */
    public void getExitBtn(ActionEvent event) throws Exception {
        System.out.println("exit server");
        System.exit(0);
    }
}