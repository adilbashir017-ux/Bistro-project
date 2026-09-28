package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import logic.Subscriber;
import navigation.Navigation;
import client.ClientUI;
import logic.Request;

import java.util.List;

/**
 * Controller for displaying a list of subscribers in a TableView.
 * <p>
 * This controller initializes the table columns, requests the subscriber list from the server,
 * and provides navigation buttons to exit the application or return to the home screen.
 * </p>
 */
public class SubscribersListController {

    /** Reference to the active controller instance. */
    public static SubscribersListController activeController;

    /**
     * Constructor that sets the active controller instance.
     */
    public SubscribersListController() {
        activeController = this;
    }

    /** Table displaying all subscribers. */
    @FXML private TableView<Subscriber> table;

    /** Column for subscriber ID. */
    @FXML private TableColumn<Subscriber, Integer> colId;

    /** Column for subscriber username. */
    @FXML private TableColumn<Subscriber, String> colName;

    /** Column for subscriber email. */
    @FXML private TableColumn<Subscriber, String> colEmail;

    /** Column for subscriber phone number. */
    @FXML private TableColumn<Subscriber, String> colPhone;

    /** Button to exit the application. */
    @FXML private Button exitBtn;

    /** Button to go back to the home screen. */
    @FXML private Button backBtn;

    /**
     * Initializes the TableView columns and requests the list of subscribers from the server.
     */
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        // Request subscribers from the server
        ClientUI.chat.sendToServer(new Request("GET_ALL_SUBSCRIBERS", null));
    }

    /**
     * Sets the list of subscribers to display in the table.
     *
     * @param list a {@link List} of {@link Subscriber} objects
     */
    public void setSubscribers(List<Subscriber> list) {
        table.getItems().setAll(list);
    }

    /**
     * Exits the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the home screen based on the user's role.
     *
     * @param event the {@link ActionEvent} triggered by clicking the back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target = Navigation.getHomeByRole();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + target));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}