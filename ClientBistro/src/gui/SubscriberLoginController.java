package gui;

import client.ClientController;
import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;
import logic.SubscriberLoginRequest;
import navigation.Navigation;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.io.File;

/**
 * Controller for the Subscriber Login screen.
 * <p>
 * Handles both normal login and QR-based login for subscribers.
 * Communicates with the server via {@link ClientController}.
 * </p>
 */
public class SubscriberLoginController {

    /** The client controller used for server communication. */
    private ClientController client;

    /** Active instance of this controller. */
    private static SubscriberLoginController active;

    /** Text field for subscriber ID input. */
    @FXML private TextField txtSubscriberId;

    /** Text field for subscriber name input. */
    @FXML private TextField txtSubscriberName;

    /** Label for showing status messages or errors. */
    @FXML private Label lblStatus;

    /**
     * Constructor. Sets this controller as the active instance.
     */
    public SubscriberLoginController() {
        active = this;
    }

    /**
     * Returns the currently active controller instance.
     *
     * @return active {@link SubscriberLoginController} instance
     */
    public static SubscriberLoginController getActive() {
        return active;
    }

    /**
     * Sets the client controller used for server communication.
     *
     * @param client the {@link ClientController} instance
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Initializes the login screen.
     * <p>
     * Clears any default status messages.
     * </p>
     */
    @FXML
    public void initialize() {
        lblStatus.setText("");
    }

    /**
     * Handles normal login when the login button is clicked.
     * <p>
     * Validates input fields and sends a {@link SubscriberLoginRequest} to the server.
     * </p>
     *
     * @param event the {@link ActionEvent} triggered by clicking the login button
     */
    @FXML
    private void onLogin(ActionEvent event) {
        lblStatus.setText("");

        String id = getTxtSubscriberId().getText();
        String name = txtSubscriberName.getText();

        if (id.isBlank() || name.isBlank()) {
            lblStatus.setText("Please fill all fields");
            return;
        }

        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            lblStatus.setText("Subscriber ID must be a number");
            return;
        }

        ClientUI.chat.sendToServer(new SubscriberLoginRequest(id.trim(), name.trim()));
    }

    /**
     * Handles QR-based login.
     * <p>
     * Opens a file chooser to select a QR code image, decodes it,
     * and sends a login request to the server.
     * </p>
     */
    @FXML
    private void onLoginWithQR() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("QR Images", "*.png")
            );

            File file = chooser.showOpenDialog(null);
            if (file == null) return;

            var image = ImageIO.read(file);
            var source = new BufferedImageLuminanceSource(image);
            var bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);

            String qrData = result.getText(); // SUBSCRIBER_ID:12

            if (!qrData.startsWith("SUBSCRIBER_ID:")) {
                lblStatus.setText("Invalid QR code");
                return;
            }

            String id = qrData.replace("SUBSCRIBER_ID:", "").trim();

            ClientUI.chat.sendToServer(
                    new Request("LOGIN_WITH_QR", id)
            );

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Failed to scan QR");
        }
    }

    /**
     * Displays a generic login error message.
     */
    public void showLoginError() {
        lblStatus.setText("Wrong Subscriber ID or Name");
    }

    /**
     * Displays a custom error message.
     *
     * @param msg the error message to show
     */
    public void showCustomError(String msg) {
        lblStatus.setText(msg);
    }

    /**
     * Handles successful login.
     * <p>
     * Opens the subscriber main screen depending on the application type.
     * </p>
     *
     * @param subscriber the logged-in {@link Subscriber}
     * @param event      the {@link ActionEvent} that triggered the login
     */
    public void showLoginSuccess(Subscriber subscriber, ActionEvent event) {
        try {
            String fxml = Navigation.getRoleSelectionScreen()
                    .equals("RoleSelectionTerminal.fxml")
                    ? "/gui/SubscriberMainTerminal.fxml"
                    : "/gui/SubscriberMainApp.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (fxml.contains("App")) {
                SubscriberMainControllerApp ctrl = loader.getController();
                ctrl.setSubscriber(subscriber);
                ctrl.setClient(client);
            } else {
                SubscriberMainController ctrl = loader.getController();
                ctrl.setSubscriber(subscriber);
                ctrl.setClient(client);
            }

            Stage stage = new Stage();
            stage.setTitle("Subscriber Dashboard");
            stage.setScene(new Scene(root));
            stage.show();

            ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the back button click.
     * <p>
     * Navigates to the main interface screen.
     * </p>
     *
     * @param event the {@link ActionEvent} triggered by clicking the back button
     */
    @FXML
    private void onBack(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Interface.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the text field for subscriber ID input.
     *
     * @return the {@link TextField} for subscriber ID
     */
    public TextField getTxtSubscriberId() {
        return txtSubscriberId;
    }

    /**
     * Sets the text field for subscriber ID input.
     *
     * @param txtSubscriberId the {@link TextField} to set
     */
    public void setTxtSubscriberId(TextField txtSubscriberId) {
        this.txtSubscriberId = txtSubscriberId;
    }
}