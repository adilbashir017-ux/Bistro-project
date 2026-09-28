package gui;

import client.ChatClient;
import client.ClientUI;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;
import navigation.Navigation;

/**
 * Controller for the Role Selection screen in TERMINAL mode.
 * <p>
 * This screen allows users to choose their role: Guest, Subscriber, Agent, or Manager.
 * Depending on the role selected, it navigates to the corresponding FXML screen.
 * </p>
 * <p>
 * Hover effects are applied to all role cards.
 * The TERMINAL mode is set automatically during initialization.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // This is handled automatically by JavaFX when loading FXML
 * FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RoleSelectionTerminal.fxml"));
 * Parent root = loader.load();
 * </pre>
 *
 * @author Mohamad
 * @version 1.0
 */
public class RoleSelectionTerminalController {

    /** Root AnchorPane of the screen */
    @FXML
    private AnchorPane anchorPane;

    /** Role cards */
    @FXML private StackPane guestCard;
    @FXML private StackPane subscriberCard;
    @FXML private StackPane agentCard;
    @FXML private StackPane managerCard;

    /** Buttons for exiting or going back */
    @FXML private Button exitBtn, backBtn;

    /**
     * Initializes the controller.
     * <p>
     * Sets TERMINAL mode and applies hover effects to all cards.
     * </p>
     */
    @FXML
    public void initialize() {
        applyHoverEffect(guestCard);
        applyHoverEffect(subscriberCard);
        applyHoverEffect(agentCard);
        applyHoverEffect(managerCard);

        Navigation.setMode(Navigation.Mode.TERMINAL);
    }

    // ==================================================
    //                    ROLE ROUTING
    // ==================================================

    /**
     * Handles Guest selection.
     * <p>
     * Navigates to TerminalReservation screen and sets the active reservation handler.
     * </p>
     */
    @FXML
    private void onGuestClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/TerminalReservation.fxml"));
            Parent root = loader.load();

            TerminalReservationController controller = loader.getController();
            controller.setClient(ClientUI.chat);
            ChatClient.setActiveReservationHandler(controller);

            anchorPane.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Subscriber selection.
     * <p>
     * Navigates to SubscriberLogin screen.
     * </p>
     */
    @FXML
    private void onSubscriberClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberLogin.fxml"));
            Parent root = loader.load();

            SubscriberLoginController controller = loader.getController();
            controller.setClient(ClientUI.chat);

            anchorPane.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Agent selection.
     * <p>
     * Navigates to RepresentativeLogin screen.
     * </p>
     */
    @FXML
    private void onAgentClicked() {
        loadScreen("RepresentativeLogin.fxml");
    }

    /**
     * Handles Manager selection.
     * <p>
     * Navigates to ManagerLogin screen.
     * </p>
     */
    @FXML
    private void onManagerClicked() {
        loadScreen("ManagerLogin.fxml");
    }

    // ==================================================
    //                    HELPERS
    // ==================================================

    /**
     * Loads a screen by FXML file name and replaces the root of the scene.
     *
     * @param fxml The name of the FXML file to load (e.g., "ManagerLogin.fxml")
     */
    private void loadScreen(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/" + fxml)
            );
            anchorPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Applies hover animation to a card (StackPane).
     * <p>
     * Scales the card slightly when mouse enters and resets on exit.
     * </p>
     *
     * @param card The StackPane card to apply the hover effect to.
     */
    private void applyHoverEffect(StackPane card) {
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });

        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
    }

    /**
     * Handles back button click.
     * <p>
     * Loads the previous interface screen.
     * </p>
     */
    @FXML
    private void back() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/gui/Interface.fxml")
            );
            anchorPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles exit button click.
     * <p>
     * Exits the application.
     * </p>
     */
    @FXML
    private void exit() {
        System.exit(0);
    }
}