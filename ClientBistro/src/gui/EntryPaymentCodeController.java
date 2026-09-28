package gui;

import client.ClientUI;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;

/**
 * JavaFX controller for entering a confirmation code before performing a payment action.
 * <p>
 * This screen collects a reservation confirmation code from the user and sends a server request
 * to validate whether the order is eligible for payment. The server response is expected to be
 * handled by the client networking layer, which may then close this window or show relevant feedback.
 * </p>
 */
public class EntryPaymentCodeController {

    /** Text field used to input the reservation confirmation code. */
    @FXML
    private TextField txtConfirmationCode;

    /**
     * The subscriber associated with this payment flow.
     * Must be set before continuing.
     */
    private Subscriber subscriber;

    /**
     * Stores the previous screen root to allow returning back if needed.
     */
    private Parent previousRoot;

    /**
     * Static reference to the currently active controller instance.
     * Used by other components (e.g., networking layer) to access this controller.
     */
    private static EntryPaymentCodeController ACTIVE;

    /**
     * Initializes the controller after the FXML has been loaded.
     * Stores this instance in {@link #ACTIVE}.
     */
    @FXML
    public void initialize() {
        ACTIVE = this;
    }

    /**
     * Returns the currently active {@link EntryPaymentCodeController} instance.
     *
     * @return the active controller instance, or {@code null} if not initialized yet
     */
    public static EntryPaymentCodeController getActive() {
        return ACTIVE;
    }

    /**
     * Sets the subscriber associated with the current payment flow.
     *
     * @param subscriber the subscriber to associate with this screen
     */
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    /**
     * Stores the previous screen root to allow returning back if needed.
     *
     * @param root the previous root node to store
     */
    public void setPreviousRoot(Parent root) {
        this.previousRoot = root;
    }

    /**
     * Validates the input confirmation code and sends a server request to check if payment is allowed.
     * <p>
     * Sends {@code "CHECK_ORDER_FOR_PAYMENT"} with the parsed confirmation code as request data.
     * </p>
     */
    @FXML
    private void onContinue() {

        if (subscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        String codeStr = txtConfirmationCode.getText();

        if (codeStr == null || codeStr.isBlank()) {
            showError("Please enter confirmation code.");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(codeStr.trim());
        } catch (NumberFormatException e) {
            showError("Confirmation code must contain numbers only.");
            return;
        }

        ClientUI.chat.sendToServer(
                new Request("CHECK_ORDER_FOR_PAYMENT", code)
        );
    }

    /**
     * Closes the current window.
     * Intended to be invoked after a successful server validation or on cancel.
     */
    public void closeWindow() {
        Stage stage = (Stage) txtConfirmationCode.getScene().getWindow();
        stage.close();
    }

    /**
     * Cancels the current action and closes the window.
     */
    @FXML
    private void onCancel() {
        closeWindow();
    }

    /**
     * Displays an error dialog to the user.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
