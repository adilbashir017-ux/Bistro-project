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
 * Controller for the manager login screen.
 * Handles manager authentication and navigation.
 */
public class ManagerLoginController {

    /** Active controller instance */
    private static ManagerLoginController active;

    /**
     * Saves reference to this controller.
     */
    public ManagerLoginController() {
        active = this;
    }

    /**
     * Returns the active controller.
     *
     * @return active ManagerLoginController
     */
    public static ManagerLoginController getActive() {
        return active;
    }


    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;
    @FXML private Button backBtn, exitBtn;



    /**
     * Called when the login button is clicked.
     * Sends login request to the server.
     */
    @FXML
    private void onLogin(ActionEvent event) {

        lblStatus.setText("");

        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isBlank() || password.isBlank()) {
            lblStatus.setText("Please fill all fields");
            return;
        }

        ClientUI.chat.sendToServer(
                new StaffLoginRequest(
                        username.trim(),
                        password.trim()
                )
        );
    }


    /**
     * Called when login is successful.
     * Opens the manager home page.
     */
    public void showLoginSuccess() {
        try {
            Navigation.setRole(Navigation.Role.MANAGER);

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ManagerHomePage.fxml"));
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
     * Shows login error message.
     */
    public void showLoginError() {
        lblStatus.setText("Wrong username or password");
    }



    /**
     * Closes the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }



    /**
     * Goes back to the role selection screen.
     *
     * @param event button click
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