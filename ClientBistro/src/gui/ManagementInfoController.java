package gui;

import java.io.IOException;

import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import logic.Request;

/**
 * Controller for the management information screen.
 * Displays general statistics for managers.
 */
public class ManagementInfoController {

    /** Active controller instance */
    public static ManagementInfoController activeController;

    /** Labels for statistics */
    @FXML private Label todayReservationsLabel;
    @FXML private Label monthlyReservationsLabel;
    @FXML private Label canceledReservationsLabel;
    @FXML private Label subscribersLabel;
    @FXML private Label currentCustomersLabel;

    /** Navigation buttons */
    @FXML private Button exitBtn, backBtn;

    /**
     * Initializes the screen and requests data from the server.
     */
    @FXML
    public void initialize() {
        activeController = this;
        ClientUI.chat.sendToServer(new Request("GET_MANAGEMENT_INFO", null));
    }

    /**
     * Updates the labels with management data.
     *
     * @param today number of today's reservations
     * @param month number of monthly reservations
     * @param canceled number of canceled reservations
     * @param subscribers number of subscribers
     * @param inside number of current customers
     */
    public void updateData(
            int today,
            int month,
            int canceled,
            int subscribers,
            int inside) {

        todayReservationsLabel.setText(String.valueOf(today));
        monthlyReservationsLabel.setText(String.valueOf(month));
        canceledReservationsLabel.setText(String.valueOf(canceled));
        subscribersLabel.setText(String.valueOf(subscribers));
        currentCustomersLabel.setText(String.valueOf(inside));
    }

    /* ================= EXIT ================= */

    /**
     * Closes the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /* ================= BACK ================= */

    /**
     * Goes back to the manager home page.
     *
     * @param event button click
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ManagerHomePage.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}