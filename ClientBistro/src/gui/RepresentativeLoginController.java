package gui;

import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import logic.StaffLoginRequest;
import navigation.Navigation;

/**
 * JavaFX controller for the representative login screen.
 * <p>
 * Handles authentication input (username/password) and sends a login request to the server.
 * After receiving the server response, the networking layer can call either
 * {@link #showLoginSuccess()} or {@link #showLoginError()} to update the UI accordingly.
 * </p>
 */
public class RepresentativeLoginController {

    /**
     * Static reference to the currently active controller instance.
     * <p>
     * Used by other components (e.g., the client networking layer) to access this controller
     * and display login results.
     * </p>
     */
    private static RepresentativeLoginController active;

    /**
     * Creates a new controller instance and stores it as the active controller.
     */
    public RepresentativeLoginController() {
        active = this;
    }

    /**
     * Returns the currently active {@link RepresentativeLoginController} instance.
     *
     * @return the active controller instance, or {@code null} if not initialized yet
     */
    public static RepresentativeLoginController getActive() {
        return active;
    }

    /** Text field for entering the username. */
    @FXML private TextField txtUsername;

    /** Password field for entering the password. */
    @FXML private PasswordField txtPassword;

    /** Label used to display validation messages and login results. */
    @FXML private Label lblStatus;

    /** Buttons for navigation and exiting the application. */
    @FXML private Button backBtn, exitBtn;

    /**
     * Handles the login action from the UI.
     * <p>
     * Validates that both username and password are provided, then sends a
     * {@link StaffLoginRequest} to the server via the client communication layer.
     * </p>
     *
     * @param event the UI action event triggered by clicking the Login button
     */
    @FXML
    private void onLogin(ActionEvent event) {

        lblStatus.setText("");

        if (txtUsername.getText().isBlank() || txtPassword.getText().isBlank()) {
            lblStatus.setText("Please fill all fields");
            return;
        }

        ClientUI.chat.sendToServer(
            new StaffLoginRequest(
                txtUsername.getText().trim(),
                txtPassword.getText().trim()
            )
        );
    }

    /**
     * Displays login success behavior after the server approves authentication.
     * <p>
     * Sets the current role to {@link navigation.Navigation.Role#AGENT} and navigates
     * to the representative homepage screen. The method also closes other open stages
     * to keep a single main window.
     * </p>
     */
    public void showLoginSuccess() {
        try {
            Navigation.setRole(Navigation.Role.AGENT);

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/RepresentativeHomepage.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            Stage.getWindows().forEach(w -> {
                if (w instanceof Stage s && s != stage) {
                    s.close();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a login failure message to the user.
     * <p>
     * Intended to be invoked after the server rejects authentication.
     * </p>
     */
    public void showLoginError() {
        lblStatus.setText("Wrong username or password");
    }

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the role selection screen.
     *
     * @param event the UI action event triggered by clicking the Back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target = Navigation.getRoleSelectionScreen();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + target));

            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
