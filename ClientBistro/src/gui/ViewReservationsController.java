package gui;

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

import java.util.List;

/**
 * Controller for viewing all reservations with subscriber details.
 */
public class ViewReservationsController {

    public static ViewReservationsController activeController;

    @FXML
    private TableView<Order> reservationsTable;

    // Existing reservation columns
    @FXML
    private TableColumn<Order, String> dateCol;
    @FXML
    private TableColumn<Order, String> timeCol;
    @FXML
    private TableColumn<Order, Integer> guestsCol;
    @FXML
    private TableColumn<Order, String> statusCol;

    // New subscriber info columns
    @FXML
    private TableColumn<Order, String> nameCol;
    @FXML
    private TableColumn<Order, String> phoneCol;
    @FXML
    private TableColumn<Order, String> emailCol;

    @FXML
    private Button backBtn;
    @FXML
    private Button exitBtn;

    private final ObservableList<Order> reservationsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        activeController = this;

        // Map columns to getter names (must match JavaFX getters!)
        dateCol.setCellValueFactory(new PropertyValueFactory<>("order_date"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("order_time"));
        guestsCol.setCellValueFactory(new PropertyValueFactory<>("number_of_guests"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("order_status"));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("customer_name"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("customer_phone"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("customer_email"));


        reservationsTable.setItems(reservationsList);

        // Request all reservations from the server
        ClientUI.chat.sendToServer(new Request("GET_ALL_RESERVATIONS", null));
    }

    /**
     * Populates the TableView with a list of reservations.
     */
    public void setReservations(List<Order> orders) {
        reservationsList.clear();
        reservationsList.addAll(orders);
    }

    @FXML
    private void exit() {
        System.exit(0);
    }

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
