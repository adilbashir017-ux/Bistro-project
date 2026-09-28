package gui;

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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Waiting List screen (Terminal).
 * <p>
 * Allows a subscriber to:
 * <ul>
 *   <li>Join the waiting list by specifying number of guests</li>
 *   <li>Leave the waiting list using a confirmation code</li>
 *   <li>Navigate back to the subscriber main screen</li>
 * </ul>

 */
public class WaitingListController {

    /* ================= FXML ================= */

    /** Text field for entering number of guests. */
    @FXML
    private TextField txtGuests;

    /** Text field for entering confirmation code. */
    @FXML
    private TextField txtConfirmationCode;

    /** Button used to join the waiting list. */
    @FXML
    private Button btnJoinWaitingList;

    /** Button used to leave the waiting list. */
    @FXML
    private Button btnLeaveWaitingList;

    /** Button to exit the application. */
    @FXML
    private Button exitBtn;

    /** Button to return to the previous screen. */
    @FXML
    private Button backBtn;

    /* ================= DATA ================= */

    /** Currently logged-in subscriber. */
    private Subscriber subscriber;

    /* ================= INIT ================= */

    /**
     * Sets the current subscriber.
     * <p>
     * Called from {@link SubscriberMainController}.
     * </p>
     *
     * @param subscriber the logged-in {@link Subscriber}
     */
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    /* ================= JOIN WAITING LIST ================= */

    /**
     * Handles joining the waiting list.
     * <p>
     * Validates input, builds a request payload,
     * and sends the join request to the server.
     * </p>
     */
    @FXML
    private void onJoinWaitingList() {

        if (subscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        if (txtGuests.getText().isBlank()) {
            showError("Please enter number of guests.");
            return;
        }

        int guests;
        try {
            guests = Integer.parseInt(txtGuests.getText().trim());
        } catch (NumberFormatException e) {
            showError("Number of guests must be a number.");
            return;
        }

        if (guests <= 0) {
            showError("Guests must be greater than 0.");
            return;
        }

        if (guests > 10) {
            showError("Guests must be less than 10.");
            return;
        }

        // Server expects a map payload
        Map<String, Object> data = new HashMap<>();
        data.put("subscriberId", subscriber.getSubscriberId());
        data.put("guests", guests);
        data.put("name", subscriber.getUsername());
        data.put("arrival",LocalDateTime.now().toString());

        ClientUI.chat.sendToServer(
                new Request("JOIN_WAITING_LIST", data)
        );
    }

    /* ================= LEAVE WAITING LIST ================= */

    /**
     * Handles leaving the waiting list using a confirmation code.
     */
    @FXML
    private void onLeaveWaitingList() {

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
     * Exits the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the Subscriber main screen.
     *
     * @param event the {@link ActionEvent} triggered by the back button
     */
    @FXML
    private void back(ActionEvent event) {

        if (subscriber == null) {
            showError("Subscriber not loaded. Please login again.");
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberMain.fxml"));

            Parent root = loader.load();

            SubscriberMainController ctrl = loader.getController();
            ctrl.setSubscriber(subscriber); // pass subscriber back

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================= HELPERS ================= */

    /**
     * Displays an error dialog.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Waiting List Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}