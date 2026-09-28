package gui;

import client.ClientController;
import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;

/**
 * Controller for the subscriber main dashboard (App version).
 * <p>
 * Allows the subscriber to make reservations, join the waiting list, pay bills,
 * view account history, see QR code, and logout.
 * </p>
 * <p>
 * Implements {@link SubscriberChildScreen} to receive subscriber and client data.
 * </p>
 */
public class SubscriberMainControllerApp implements SubscriberChildScreen {

    /** Client controller for server communication. */
    private ClientController client;

    /** Label for the welcome message. */
    @FXML
    private Label lblWelcome;

    /** The logged-in subscriber. */
    private Subscriber subscriber;

    /* ================== INIT ================== */

    /**
     * Sets the client controller for server communication.
     *
     * @param client the {@link ClientController} instance
     */
    @Override
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Sets the subscriber and updates the welcome label.
     *
     * @param subscriber the logged-in {@link Subscriber}, may be null
     */
    @Override
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;

        if (subscriber == null) {
            lblWelcome.setText("Welcome");
            return;
        }

        lblWelcome.setText(
                "Welcome, " +
                        subscriber.getUsername() +
                        " (ID: " +
                        subscriber.getSubscriberId() +
                        ")"
        );
    }

    /* ================== BUTTON HANDLERS ================== */

    /**
     * Opens the "Make Reservation" screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onMakeReservation(ActionEvent event) {
        openScreenWithSubscriber("/gui/SubscriberReservationApp.fxml",
                "Make a Reservation", event);
    }

    /**
     * Opens the "Waiting List" screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onJoinWaitingList(ActionEvent event) {
        openScreenWithSubscriber("/gui/WaitingListApp.fxml",
                "Waiting List", event);
    }

    /**
     * Opens the "Pay Bill" screen.
     */
    @FXML
    private void onPayBill() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/EntrypaymentCode.fxml"));
            Parent root = loader.load();

            EntryPaymentCodeController ctrl = loader.getController();
            ctrl.setSubscriber(subscriber);

            Stage stage = new Stage();
            stage.setTitle("Enter Confirmation Code");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the subscriber account history screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onMyAccount(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/AccountHistory.fxml"));
            Parent root = loader.load();

            AccountHistoryController ctrl = loader.getController();
            ctrl.setSubscriber(subscriber);
            ctrl.setPreviousFXML("/gui/SubscriberMainApp.fxml");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("My Account");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Account History.");
        }
    }

    /**
     * Logs the subscriber out and opens the login screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the logout button
     */
    @FXML
    private void onLogout(ActionEvent event) {
        ClientUI.chat.sendToServer(new Request("SUBSCRIBER_LOGOUT", null));
        openScreenNoSubscriber("/gui/SubscriberLogin.fxml",
                "Subscriber Login", event);
    }

    /**
     * Opens the subscriber QR code screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onShowQRCode(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberQR.fxml"));
            Parent root = loader.load();

            SubscriberQRController ctrl = loader.getController();
            ctrl.setSubscriber(subscriber);

            Stage stage = new Stage();
            stage.setTitle("My QR Code");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open QR Code screen.");
        }
    }

    /* ================== NAVIGATION HELPERS ================== */

    /**
     * Opens a new screen and passes the subscriber to it.
     *
     * @param fxml  the FXML file path
     * @param title the title of the new stage
     * @param event the {@link ActionEvent} that triggered the navigation
     */
    private void openScreenWithSubscriber(String fxml, String title, ActionEvent event) {
        if (subscriber == null) {
            showError("Subscriber not loaded. Please login again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof SubscriberChildScreen) {
                ((SubscriberChildScreen) controller).setSubscriber(subscriber);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open screen: " + fxml);
        }
    }

    /**
     * Opens a screen that does not require a subscriber.
     *
     * @param fxml  the FXML file path
     * @param title the title of the new stage
     * @param event the {@link ActionEvent} that triggered the navigation
     */
    private void openScreenNoSubscriber(String fxml, String title, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

            closeCurrentStage(event);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open screen: " + fxml);
        }
    }

    /**
     * Closes the current stage.
     *
     * @param event the {@link ActionEvent} that triggered the closure
     */
    private void closeCurrentStage(ActionEvent event) {
        ((Stage) ((Node) event.getSource())
                .getScene().getWindow()).close();
    }

    /**
     * Shows an error alert with the given message.
     *
     * @param msg the error message
     */
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }
}