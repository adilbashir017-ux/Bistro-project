package gui;

import client.ChatClient;
import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import navigation.Navigation;

/**
 * Controller for the manager home page.
 * Provides navigation to all manager-related screens.
 */
public class ManagerHomePageController {

    /** Main container */
    @FXML
    private AnchorPane anchorPane;

    /** Navigation buttons */
    @FXML private Button exitBtn, backBtn;



    /**
     * Loads a target screen.
     *
     * @param event button click event
     * @param targetFxml target fxml file
     */
    private void go(ActionEvent event, String targetFxml) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/gui/" + targetFxml)
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

    /** Opens subscriber registration screen */
    @FXML
    private void onRegisterSubscriber(ActionEvent event) {
        go(event, "RegisterSubscriber.fxml");
    }

    /** Opens subscribers list screen */
    @FXML
    void onSubscribers(ActionEvent event) {
        go(event, "SubscribersList.fxml");
    }

    /** Opens current customers screen */
    @FXML
    void onCurrentCustomers(ActionEvent event) {
        go(event, "CurrentCustomers.fxml");
    }

    /** Opens active reservations screen */
    @FXML
    void onActiveReservations(ActionEvent event) {
        go(event, "ActiveReservations.fxml");
    }

    /** Opens table management screen */
    @FXML
    private void onManageTables(ActionEvent event) {
        go(event, "TableManagement.fxml");
    }

    /** Opens reservation viewing screen */
    @FXML
    private void onViewReservations(ActionEvent event) {
        go(event, "ViewReservation.fxml");
    }

    /** Opens opening hours screen */
    @FXML
    private void onOpeningHours(ActionEvent event) {
        go(event, "OpeningHours.fxml");
    }

    /** Opens waiting list screen */
    @FXML
    private void onWaitingList(ActionEvent event) {
        go(event, "ShowWaitingList.fxml");
    }

    /** Opens management information screen */
    @FXML
    private void onViewingManagementInformation(ActionEvent event) {
        go(event, "ManagementInfo.fxml");
    }

    /** Opens reports and analysis screen */
    @FXML
    private void onReportsAndAnalysis(ActionEvent event) {
        go(event, "ReportsAnalysis.fxml");
    }

    /** Opens subscriber dashboard */
    @FXML
    private void onSubscriberDashboard(ActionEvent event) {
        go(event, "SubscriberLogin.fxml");
    }

    /** Opens guest dashboard (terminal reservation) */
    @FXML
    private void onGuestDashboard(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/TerminalReservation.fxml"));
            Parent root = loader.load();

            TerminalReservationController controller = loader.getController();
            controller.setClient(ClientUI.chat);
            ChatClient.setActiveReservationHandler(controller);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
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



    /**
     * Closes the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }
}