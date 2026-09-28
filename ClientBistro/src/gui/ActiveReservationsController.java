package gui;

import client.ClientUI;
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
 * Controller for the active reservations screen.
 * Displays a list of current reservations.
 */
public class ActiveReservationsController {

    /** Reference to the active controller */
    public static ActiveReservationsController activeController;

    /**
     * Constructor – saves reference to this controller.
     */
    public ActiveReservationsController() {
        activeController = this;
    }

    /** Table showing active reservations */
    @FXML private TableView<Order> table;

    /** Table columns */
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colTime;
    @FXML private TableColumn<Order, Integer> colGuests;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Integer> colTable;

    /** Navigation buttons */
    @FXML private Button backBtn, exitBtn;

    /**
     * Initializes the table and requests active reservations from server.
     */
    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("order_date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("order_time"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("number_of_guests"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("order_status"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("table_num"));

        ClientUI.chat.sendToServer(
                new Request("GET_ACTIVE_RESERVATIONS", null)
        );
    }

    /**
     * Updates the table with active reservations.
     *
     * @param list list of active reservations
     */
    public void setReservations(List<Order> list) {
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
     * Goes back to the home screen according to the user role.
     *
     * @param event button click event
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target = Navigation.getHomeByRole();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + target));

            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}