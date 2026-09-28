package gui;

import client.ClientController;
import client.ClientUI;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import logic.*;

import java.util.List;

/**
 * Controller for the Account History screen.
 * <p>
 * This controller displays the subscriber's reservation history
 * and visit history using two separate tables.
 * It communicates with the server to fetch history data and
 * handles navigation back to the previous subscriber screen.
 * </p>
 */
public class AccountHistoryController implements SubscriberChildScreen {



    /** Table displaying reservation history */
    @FXML private TableView<Order> reservationsTable;

    /** Table displaying visit history */
    @FXML private TableView<Order> visitsTable;


    /** Reservation ID column */
    @FXML private TableColumn<Order, Integer> colResId;

    /** Reservation date column */
    @FXML private TableColumn<Order, String> colResDate;

    /** Number of guests column */
    @FXML private TableColumn<Order, Integer> colGuests;

    /** Reservation status column */
    @FXML private TableColumn<Order, String> colResStatus;

    /* ===== VISITS COLUMNS ===== */

    /** Visit date column */
    @FXML private TableColumn<Order, String> colVisitDate;

    /** Number of people in visit column */
    @FXML private TableColumn<Order, Integer> colVisitPeople;

    /** Visit status column */
    @FXML private TableColumn<Order, String> colVisitStatus;

    /** Back navigation button */
    @FXML private Button btnBack;

    /** Client controller used for server communication */
    private ClientController client;

    /** Currently logged-in subscriber */
    private Subscriber subscriber;

    /** Path to the previous FXML screen (for navigation) */
    private String previousFXML;

    /** Static reference to the currently active controller */
    private static AccountHistoryController ACTIVE;



    /**
     * Initializes the controller.
     * <p>
     * Sets up table column bindings and back button behavior.
     * This method is automatically called by JavaFX after FXML loading.
     * </p>
     */
    @FXML
    public void initialize() {
        ACTIVE = this;


        colResId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getOrder_number()).asObject());

        colResDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getOrder_date()));

        colGuests.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getNumber_of_guests()).asObject());

        colResStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getOrder_status()));


        colVisitDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getOrder_date()));

        colVisitPeople.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getNumber_of_guests()).asObject());

        colVisitStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getOrder_status()));

        btnBack.setOnAction(e -> goBack());
    }

    /**
     * Returns the currently active AccountHistoryController instance.
     *
     * @return the active controller
     */
    public static AccountHistoryController getActive() {
        return ACTIVE;
    }



    /**
     * Sets the client controller for server communication.
     *
     * @param client the client controller
     */
    @Override
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Sets the subscriber context and requests account history data.
     * <p>
     * Sends requests to the server for both reservation history
     * and visit history of the subscriber.
     * </p>
     *
     * @param subscriber the logged-in subscriber
     */
    @Override
    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;


        ClientUI.chat.sendToServer(
            new Request("ACCOUNT_RESERVATIONS", subscriber.getSubscriberId())
        );

        ClientUI.chat.sendToServer(
            new Request("ACCOUNT_VISITS", subscriber.getSubscriberId())
        );
    }

    /**
     * Sets the previous FXML screen path for back navigation.
     *
     * @param fxml the previous screen FXML path
     */
    public void setPreviousFXML(String fxml) {
        this.previousFXML = fxml;
    }



    /**
     * Handles reservation history data received from the server.
     *
     * @param reservations list of reservation orders
     */
    public void handleReservationHistory(List<Order> reservations) {
        Platform.runLater(() -> {
            reservationsTable.getItems().clear();
            reservationsTable.setItems(FXCollections.observableArrayList(reservations));
        });
    }

    /**
     * Handles visit history data received from the server.
     *
     * @param visits list of visit orders
     */
    public void handleVisitHistory(List<Order> visits) {
        Platform.runLater(() -> {
            visitsTable.getItems().clear();
            visitsTable.setItems(FXCollections.observableArrayList(visits));
        });
    }



    /**
     * Navigates back to the previous screen.
     * <p>
     * Loads the previous FXML file and restores the subscriber context.
     * </p>
     */
    private void goBack() {
        try {
            if (previousFXML == null) {
                showError("No previous screen.");
                return;
            }

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(previousFXML));

            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof SubscriberChildScreen) {
                ((SubscriberChildScreen) controller).setSubscriber(subscriber);
            }

            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to go back.");
        }
    }

    /**
     * Displays an error alert dialog.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }
}