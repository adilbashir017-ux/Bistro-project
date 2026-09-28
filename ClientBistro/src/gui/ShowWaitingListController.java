package gui;

import java.util.List;

import client.ClientUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import logic.Order;
import logic.Request;
import navigation.Navigation;



/**
 * Controller for displaying and managing the waiting list of orders in the restaurant.
 * <p>
 * This class is responsible for initializing the TableView and its columns,
 * receiving the waiting list from the server, and updating the table.
 * It also provides methods to navigate back to the previous page or exit the application.
 * </p>
 * <p>
 * The {@code activeController} static field allows global access to the current instance.
 * </p>
 */
public class ShowWaitingListController {

    /**
     * Static reference to the active controller instance.
     */
    public static ShowWaitingListController activeController;

    /**
     * Default constructor.
     * <p>
     * Sets {@link #activeController} to this instance.
     * </p>
     */
    public ShowWaitingListController() {
        activeController = this;
    }

    /** Table displaying all waiting orders. */
    @FXML
    private TableView<Order> waitingTable;

    /** Column showing the order numbers. */
    @FXML
    private TableColumn<Order, Integer> orderNumberCol;

    /** Column showing the order dates. */
    @FXML
    private TableColumn<Order, String> dateCol;

    /** Column showing the order times. */
    @FXML
    private TableColumn<Order, String> timeCol;

    /** Column showing the number of guests in the order. */
    @FXML
    private TableColumn<Order, Integer> guestsCol;

    /** Column showing the current status of each order. */
    @FXML
    private TableColumn<Order, String> statusCol;

    /** Buttons for navigating back or exiting the application. */
    @FXML
    private Button backBtn, exitBtn;

    /** Internal list of orders to be displayed in the TableView. */
    private final ObservableList<Order> data = FXCollections.observableArrayList();

    /**
     * Initializes the TableView and its columns.
     * <p>
     * This method is automatically called by JavaFX when the FXML is loaded.
     * It sets up the cell value factories for all columns and requests
     * the current waiting list from the server.
     * </p>
     */
    @FXML
    public void initialize() {
        orderNumberCol.setCellValueFactory(new PropertyValueFactory<>("order_number"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("order_date"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("order_time"));
        guestsCol.setCellValueFactory(new PropertyValueFactory<>("number_of_guests"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("order_status"));

        waitingTable.setItems(data);

        // Request waiting list from server
        ClientUI.chat.sendToServer(new Request("GET_WAITING_LIST", null));
    }

    /**
     * Updates the TableView with a new waiting list.
     * <p>
     * Called by the ClientUI when the server responds with the current waiting orders.
     * </p>
     *
     * @param list the list of {@link Order} objects to display
     */
    public void setWaitingList(List<Order> list) {
        data.setAll(list);
    }

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the home page based on the user's role.
     * <p>
     * Loads the correct FXML for the home page and switches the current scene.
     * </p>
     *
     * @param event triggered by clicking the back button
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