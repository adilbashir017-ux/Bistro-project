package gui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import logic.Request;
import logic.Subscriber;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import navigation.Navigation;

/**
 * JavaFX controller for the "Register Subscriber" screen.
 * <p>
 * Allows a staff user (e.g., representative/manager) to create a new subscriber by submitting
 * the subscriber details to the server. After successful creation, the controller generates
 * and displays a QR code that encodes the subscriber identifier.
 * </p>
 * <p>
 * The controller keeps a static reference to the active instance to allow server callbacks
 * (e.g., when the server confirms creation) to update the UI.
 * </p>
 */
public class RegisterSubscriberController {

    /**
     * A static reference to the currently loaded controller instance.
     * <p>
     * Used by other components (such as the client networking layer) to access this controller
     * and update the UI after server responses.
     * </p>
     */
    public static RegisterSubscriberController activeController;

    /** Input field for the subscriber's full name. */
    @FXML private TextField nameField;

    /** Input field for the subscriber's phone number. */
    @FXML private TextField phoneField;

    /** Input field for the subscriber's email address. */
    @FXML private TextField emailField;

    /** Label used to display validation messages and operation results. */
    @FXML private Label resultLabel;

    /** Image view used to display the generated QR code to the user. */
    @FXML private ImageView qrImageView;

    /** Navigation buttons for exiting the app or returning to the previous screen. */
    @FXML private Button exitBtn, backBtn;

    /**
     * Initializes the controller after the FXML has been loaded.
     * <p>
     * This method is called automatically by JavaFX and stores a reference to this instance
     * in {@link #activeController}.
     * </p>
     */
    @FXML
    public void initialize() {
        activeController = this;
    }

    // ================= CREATE SUBSCRIBER =================

    /**
     * Creates a new subscriber using the details entered by the user.
     * <p>
     * Validates that all required fields are filled. If valid, sends a request to the server
     * to register the subscriber and displays a status message to the user.
     * </p>
     * <p>
     * The server response is expected to trigger a callback (e.g., {@link #showSuccess(String)})
     * to complete the flow.
     * </p>
     */
    @FXML
    private void createSubscriber() {

        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            resultLabel.setText("Please fill all fields");
            return;
        }

        Subscriber sub = new Subscriber(0, name, email, phone);

        ClientUI.chat.sendToServer(
                new Request("REGISTER_SUBSCRIBER", sub)
        );

        resultLabel.setText("Creating subscriber...");
    }

    // ================= SERVER CALLBACK =================

    /**
     * Callback invoked after the server confirms that the subscriber was created successfully.
     * <p>
     * Generates and displays a QR code that encodes the subscriber identifier.
     * </p>
     *
     * @param displayId the subscriber identifier returned from the server (for display and QR encoding)
     */
    public void showSuccess(String displayId) {
        generateQRCode(displayId);
    }

    // ================= QR GENERATION =================

    /**
     * Generates a QR code image for the given subscriber identifier and displays it in the UI.
     * <p>
     * The QR content is encoded as {@code "SUBSCRIBER_ID:<id>"} and saved as a PNG file in the
     * current working directory with the name {@code qr_<id>.png}. The generated image is then
     * loaded and displayed in {@link #qrImageView}.
     * </p>
     *
     * @param subscriberId the subscriber identifier to encode inside the QR code
     */
    private void generateQRCode(String subscriberId) {
        try {
            String data = "SUBSCRIBER_ID:" + subscriberId;

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(data, BarcodeFormat.QR_CODE, 250, 250);

            Path path = Paths.get("qr_" + subscriberId + ".png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);

            Image qrImage = new Image(path.toUri().toString());
            qrImageView.setImage(qrImage);

            resultLabel.setText(
                "Subscriber created!\nID: " + subscriberId +
                "\nQR saved as: " + path.getFileName()
            );

        } catch (Exception e) {
            e.printStackTrace();
            resultLabel.setText("Subscriber created, but QR failed.");
        }
    }

    // ================= EXIT =================

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    // ================= BACK (SMART NAVIGATION) =================

    /**
     * Navigates back to the appropriate screen based on the interface mode.
     * <p>
     * If the role selection screen indicates terminal mode, navigates to the representative homepage.
     * Otherwise, navigates to the manager home page.
     * </p>
     *
     * @param event the UI action event triggered by clicking the Back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target;

            if (Navigation.getRoleSelectionScreen()
                    .equals("RoleSelectionTerminal.fxml")) {

                // Came from Terminal mode
                target = "RepresentativeHomepage.fxml";

            } else {
                // Came from App mode
                target = "ManagerHomePage.fxml";
            }

            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/" + target)
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
