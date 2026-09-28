package gui;

import client.ClientUI;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

/**
 * Controller for the main interface screen.
 * Allows the user to choose between terminal mode and app mode.
 */
public class InterfaceController {

    /** Main container */
    @FXML
    private AnchorPane anchorPane;

    /** Terminal option card */
    @FXML private StackPane terminalCard;

    /** App option card */
    @FXML private StackPane appCard;

    /**
     * Initializes the screen and applies hover effects.
     */
    @FXML
    public void initialize() {
        applyHoverEffect(terminalCard);
        applyHoverEffect(appCard);
    }

    /* ================= ROLE SELECTION ================= */

    /**
     * Called when the terminal option is clicked.
     */
    @FXML
    private void onTerminalClicked() {
        loadScreen("RoleSelectionTerminal.fxml");
    }

    /**
     * Called when the app option is clicked.
     */
    @FXML
    private void onAppClicked() {
        loadScreen("RoleSelectionApp.fxml");
    }

    /**
     * Loads a new screen into the main pane.
     *
     * @param fxml fxml file to load
     */
    private void loadScreen(String fxml) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            anchorPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a simple hover effect to a card.
     *
     * @param card the card to apply the effect to
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
}