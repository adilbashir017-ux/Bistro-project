package gui;

import client.ClientController;
import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;

/**
 * Controller for the Waiting List screen (App).
 * <p>
 * Allows a subscriber to leave the waiting list by entering a confirmation code.
 * Provides navigation back to the subscriber main screen and application exit.
 * </p>
 */
public class WaitingListAppController implements SubscriberChildScreen {

    /** Text field for entering the waiting list confirmation code. */
    @FXML
    private TextField txtConfirmationCode;

    /** Button used to leave the waiting list. */
    @FXML
    private Button btnLeaveWaitingList;

    /** Button to exit the application. */
    @FXML
    private Button exitBtn;

    /** Button to return to the previous screen. */
    @FXML
    private Button backBtn;

    /** Currently logged-in subscriber. */
    private Subscriber subscriber;

    /* ================= REQUIRED ================= */

    /**
     * Sets the client controller.
     * <p>
     * Not used in this screen but required by {@link SubscriberChildScreen}.
     * </p>
     *
     * @param client the {@link ClientController} instance
     */
    @Override
    public void setClient(ClientController client) {
        // Not needed for this screen
    }

    /**
     * Sets the current subscriber.
     *
     * @param subscriber the {@link Subscriber} currently logged in
     */
    @Override
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    /* ================= LEAVE WAITING LIST ================= */

    /**
     * Handles leaving the waiting list.
     * <p>
     * Validates subscriber existence and confirmation code,
     * then sends a leave request to the server.
     * </p>
     */
    @FXML
    private void onLeaveWaitingList() {

        if (subscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        if (txtConfirmationCode.getText().isBlank()) {
            showError("Please enter confirmation code.");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(txtConfirmationCode.getText().trim());
        } catch (NumberFormatException e) {
            showError("Confirmation code must be numbers only.");
            return;
        }

        ClientUI.chat.sendToServer(
                new Request("LEAVE_WAITING_LIST", code)
        );
    }

    /* ================= NAVIGATION ================= */

    /**
     * Navigates back to the Subscriber main screen.
     *
     * @param event the {@link ActionEvent} triggered by the back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberMainApp.fxml"));
            Parent root = loader.load();

            SubscriberMainControllerApp ctrl = loader.getController();
            ctrl.setSubscriber(subscriber); // pass subscriber back

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Exits the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Displays an error dialog.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Waiting List Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}