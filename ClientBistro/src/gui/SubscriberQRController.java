package gui;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import logic.Subscriber;
import logic.QRService;

import java.io.ByteArrayInputStream;

/**
 * Controller for displaying a subscriber's QR code.
 * <p>
 * This controller receives a {@link Subscriber} object, generates a QR code image
 * representing the subscriber's ID, and displays it in the {@link ImageView}.
 * </p>
 */
public class SubscriberQRController {

    /** The ImageView where the QR code will be displayed. */
    @FXML
    private ImageView imgQRCode;

    /** The subscriber whose QR code is displayed. */
    private Subscriber subscriber;

    /**
     * Sets the subscriber and automatically generates the QR code for display.
     *
     * @param subscriber the {@link Subscriber} whose QR code will be generated
     */
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
        generateQR();
    }

    /**
     * Generates a QR code image for the current subscriber and sets it in the ImageView.
     * <p>
     * The QR code is generated as a byte array using {@link logic.QRService#generateQRImageBytes(String)}
     * and then converted to a JavaFX {@link Image} for display.
     * </p>
     * <p>
     * If the subscriber is null, this method does nothing.
     * </p>
     */
    private void generateQR() {
        try {
            if (subscriber == null) return;

            // Get QR as byte array from your logic class
            // Example: QRLogic.generateQR(subscriber.getSubscriberId());
            byte[] qrBytes = QRService.generateQRImageBytes(subscriber.getSubscriberId() + "");

            Image qrImage = new Image(new ByteArrayInputStream(qrBytes));
            imgQRCode.setImage(qrImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}