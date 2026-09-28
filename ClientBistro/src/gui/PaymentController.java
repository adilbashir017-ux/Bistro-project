package gui;

import client.ChatClient;
import client.ClientUI;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import logic.Request;
import logic.Subscriber;

/**
 * Controller for handling subscriber payment in the GUI.
 * <p>
 * Displays the order details, calculates discounts, and allows
 * the user to finalize payment. Communicates with the server
 * via {@link ClientUI} to process payments.
 * </p>
 * <p>
 * This controller is linked to the active {@link ChatClient} instance
 * for receiving callbacks from the server.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PaymentController controller = loader.getController();
 * controller.setData(subscriber, confirmationCode, totalPrice);
 * </pre>
 *
 * @author Mohamad
 * @version 1.0
 */
public class PaymentController {

    /** Label displaying subscriber name */
    @FXML private Label lblName;

    /** Label displaying the confirmation code */
    @FXML private Label lblOrder;

    /** Label displaying the original price */
    @FXML private Label lblOriginal;

    /** Label displaying the discount applied */
    @FXML private Label lblDiscount;

    /** Label displaying the final price to pay */
    @FXML private Label lblFinal;

    /** Confirmation code for the current order */
    private int confirmationCode;

    /** Subscriber making the payment */
    private Subscriber subscriber;

    // ================= INIT =================

    /**
     * Initializes the controller.
     * <p>
     * Registers this controller as the active payment controller
     * in {@link ChatClient} for server callbacks.
     * </p>
     */
    @FXML
    public void initialize() {
        ChatClient.setActivePaymentController(this);
    }

    // ================= DATA =================

    /**
     * Sets the subscriber and payment details for the UI.
     * <p>
     * Calculates a 10% discount and displays all relevant information
     * in the labels.
     * </p>
     *
     * @param sub The subscriber
     * @param confirmationCode The confirmation code for the order
     * @param price The original order price
     */
    public void setData(Subscriber sub, int confirmationCode, double price) {
        this.subscriber = sub;
        this.confirmationCode = confirmationCode;

        double discount = price * 0.10;
        double finalPrice = price - discount;

        if (sub != null) {
            lblName.setText("Subscriber: " + sub.getUsername());
        } else {
            lblName.setText("Subscriber");
        }

        lblOrder.setText("Confirmation Code: " + confirmationCode);
        lblOriginal.setText("Original Price: ₪" + price);
        lblDiscount.setText("Discount (10%): -₪" + discount);
        lblFinal.setText("Total to Pay: ₪" + finalPrice);
    }

    // ================= PAY =================

    /**
     * Called when the user clicks the Pay button.
     * <p>
     * Sends a {@link Request} to the server to finalize payment.
     * </p>
     */
    @FXML
    private void onPay() {
        ClientUI.chat.sendToServer(
                new Request("PAY_BILL", confirmationCode)
        );
    }

    // ================= SERVER CALLBACKS =================

    /**
     * Called when the server confirms a successful payment.
     * <p>
     * Displays an information alert and closes the window.
     * </p>
     */
    public void showPaymentSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Successful");
        alert.setHeaderText("Thank you for your payment. Checkout sent to your email ✅");
        alert.setContentText("Payment completed successfully.\nEnjoy your meal!");
        alert.showAndWait();
        closeWindow();
    }

    /**
     * Called when the server indicates a failed payment.
     * <p>
     * Displays an error alert to the user.
     * </p>
     */
    public void showPaymentFailed() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Payment Failed");
        alert.setHeaderText(null);
        alert.setContentText("Payment could not be completed.\nPlease contact staff.");
        alert.showAndWait();
    }

    // ================= BACK / CLOSE =================

    /**
     * Called when the Back button is clicked.
     * <p>
     * Closes the payment window without making any changes.
     * </p>
     */
    @FXML
    private void onBack() {
        closeWindow();
    }

    /**
     * Closes the current stage/window.
     */
    private void closeWindow() {
        Stage stage = (Stage) lblFinal.getScene().getWindow();
        stage.close();
    }
}