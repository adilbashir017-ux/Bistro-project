package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import navigation.Navigation;

/**
 * Controller for the Representative (Agent) Homepage.
 * <p>
 * Handles navigation to different screens for managing subscribers,
 * reservations, tables, and other administrative tasks.
 * </p>
 * <p>
 * Provides methods for back navigation and exiting the application.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RepresentativeHomepage.fxml"));
 * Parent root = loader.load();
 * RepresentativeHomepageController controller = loader.getController();
 * </pre>
 *
 * @author Mohamad
 * @version 1.0
 */
public class RepresentativeHomepageController {

    /** Back button in the GUI */
    @FXML private Button backBtn;

    /** Exit button in the GUI */
    @FXML private Button exitBtn;

    /* ================= SIMPLE NAVIGATION ================= */

    /**
     * Switches the scene to the given FXML file.
     *
     * @param event ActionEvent triggered by the button
     * @param fxml  Name of the FXML file to load
     */
    private void switchScene(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/" + fxml)
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= BUTTONS ================= */

    /**
     * Navigate to the subscriber registration screen.
     */
    @FXML
    void onRegisterSubscriber(ActionEvent event) {
        switchScene(event, "RegisterSubscriber.fxml");
    }

    /**
     * Navigate to the list of subscribers.
     */
    @FXML
    void onSubscribers(ActionEvent event) {
        switchScene(event, "SubscribersList.fxml");
    }

    /**
     * Open the Active Reservations screen in a new window.
     */
    @FXML
    void onActiveReservations(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/ActiveReservations.fxml")
            );

            // Get the current window (Stage) from the button that triggered this event
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

            // Replace the current scene with the new one
            stage.setTitle("Active Reservations");
            stage.setScene(new Scene(root));
            stage.show(); // optional, stage is already showing

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Navigate to the current customers screen.
     */
    @FXML
    void onCurrentCustomers(ActionEvent event) {
        switchScene(event, "CurrentCustomers.fxml");
    }

    /**
     * Navigate to the table management screen.
     */
    @FXML
    void onManageTables(ActionEvent event) {
        switchScene(event, "TableManagement.fxml");
    }

    /**
     * Navigate to the reservations viewing screen.
     */
    @FXML
    void onViewReservations(ActionEvent event) {
        switchScene(event, "ViewReservation.fxml");
    }

    /**
     * Navigate to the opening hours management screen.
     */
    @FXML
    void onOpeningHours(ActionEvent event) {
        switchScene(event, "OpeningHours.fxml");
    }

    /**
     * Navigate to the waiting list screen.
     */
    @FXML
    void onWaitingList(ActionEvent event) {
        switchScene(event, "ShowWaitingList.fxml");
    }

    /**
     * Navigate to the subscriber dashboard login.
     */
    @FXML
    private void onSubscriberDashboard(ActionEvent event) {
        switchScene(event, "SubscriberLogin.fxml");
    }

    /**
     * Navigate to the guest dashboard (terminal reservation).
     */
    @FXML
    private void onGuestDashboard(ActionEvent event) {
        switchScene(event, "TerminalReservation.fxml");
    }

    /* ================= BACK ================= */

    /**
     * Navigate back to the role selection screen.
     *
     * @param event ActionEvent triggered by the back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target = Navigation.getRoleSelectionScreen();
            System.out.println("BACK TO: " + target);

            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/" + target)
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= EXIT ================= */

    /**
     * Exits the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }
}