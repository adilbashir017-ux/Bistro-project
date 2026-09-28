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
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;

/**
 * Controller for the main subscriber dashboard.
 * <p>
 * Allows the subscriber to make reservations, join the waiting list,
 * pay bills, view account history, and logout.
 * </p>
 * <p>
 * Implements {@link SubscriberChildScreen} to receive subscriber and client information.
 * </p>
 */
public class SubscriberMainController implements SubscriberChildScreen {

    /** Label to display a welcome message. */
    @FXML
    private Label lblWelcome;

    /** Label to display the subscriber ID. */
    @FXML
    private Label lblSubscriberId;

    /** Client controller used for server communication. */
    private ClientController client;

    /** The logged-in subscriber. */
    private Subscriber subscriber;

    /* ================== INIT ================== */

    /**
     * Sets the client controller.
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
     * @param subscriber the logged-in {@link Subscriber}
     */
    @Override
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;

        lblWelcome.setText(
                "Welcome, " +
                subscriber.getUsername() +
                " (ID: " +
                subscriber.getSubscriberId() +
                ")"
        );
    }

    /* ================== BUTTONS ================== */

    /**
     * Opens the "Make Reservation" screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onMakeReservation(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberReservation.fxml"));
            Parent root = loader.load();

            SubscriberReservationController ctrl = loader.getController();

            // Pass subscriber to reservation controller
            ctrl.setSubscriber(subscriber);

            Stage stage = new Stage();
            stage.setTitle("Make a Reservation");
            stage.setScene(new Scene(root));
            stage.show();

            closeCurrentStage(event);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================== WAITING LIST ================== */

    /**
     * Opens the waiting list screen for the subscriber.
     *
     * @param event the {@link ActionEvent} triggered by clicking the button
     */
    @FXML
    private void onJoinWaitingList(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/WaitingList.fxml"));
            Parent root = loader.load();

            WaitingListController ctrl = loader.getController();

            // Pass subscriber to waiting list controller
            ctrl.setSubscriber(subscriber);

            Stage stage = new Stage();
            stage.setTitle("Waiting List");
            stage.setScene(new Scene(root));
            stage.show();

            closeCurrentStage(event);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================== PAY BILL ================== */

    /**
     * Opens the payment screen where the subscriber enters a confirmation code.
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

    /* ================== ACCOUNT ================== */

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
            ctrl.setPreviousFXML("/gui/SubscriberMain.fxml");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("My Account");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================== LOGOUT ================== */

    /**
     * Logs the subscriber out and returns to the login screen.
     *
     * @param event the {@link ActionEvent} triggered by clicking the logout button
     */
    @FXML
    private void onLogout(ActionEvent event) {
        ClientUI.chat.sendToServer(new Request("SUBSCRIBER_LOGOUT", null));

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberLogin.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Subscriber Login");
            stage.setScene(new Scene(root));
            stage.show();

            closeCurrentStage(event);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================== HELPERS ================== */

    /**
     * Closes the current stage.
     *
     * @param event the {@link ActionEvent} that triggered this closure
     */
    private void closeCurrentStage(ActionEvent event) {
        ((Stage) ((Node) event.getSource())
                .getScene().getWindow()).close();
    }
}