package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Controller class for the Server GUI.
 * <p>
 * This class is responsible for managing the server's graphical interface,
 * including redirecting console output to a {@link TextArea} and appending
 * log messages.
 * </p>
 */
public class ServerGUI {

    /**
     * TextArea component in the GUI used as the server console.
     */
    @FXML
    private TextArea console;

    /**
     * Initializes the GUI.
     * <p>
     * Redirects the standard output and error streams to the GUI console
     * and appends a startup message.
     * </p>
     */
    @FXML
    public void initialize() {
        redirectConsoleToGUI();
        appendLog("Server GUI Loaded...\n");
    }

    /**
     * Appends a log message to the GUI console.
     * <p>
     * This method ensures that the update occurs on the JavaFX application thread.
     * </p>
     *
     * @param msg the message to append
     */
    public void appendLog(String msg) {
        Platform.runLater(() -> console.appendText(msg));
    }

    /**
     * Redirects the standard output and error streams to the GUI console.
     * <p>
     * After calling this method, any calls to {@link System#out} or {@link System#err}
     * will appear in the {@link TextArea} of the GUI instead of the standard console.
     * </p>
     */
    private void redirectConsoleToGUI() {

        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                Platform.runLater(() ->
                        console.appendText(String.valueOf((char) b)));
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
}