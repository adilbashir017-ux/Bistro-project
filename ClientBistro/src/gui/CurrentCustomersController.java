package gui;

import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
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
 * Controller for the current customers screen.
 * Displays customers that are currently in the restaurant.
 */
public class CurrentCustomersController {

    /** Reference to the active controller */
    public static CurrentCustomersController activeController;

    /**
     * Constructor – saves reference to this controller.
     */
    public CurrentCustomersController() {
        activeController = this;
    }

    /** Table showing current customers */
    @FXML private TableView<Order> table;

    /** Table columns */
    @FXML private TableColumn<Order, Integer> colName;
    @FXML private TableColumn<Order, Integer> colEmail;
    @FXML private TableColumn<Order, String> colPhone;
    @FXML private TableColumn<Order, Integer> colGuests;

    /** Navigation buttons */
    @FXML private Button backBtn, exitBtn;

    /**
     * Initializes the table and requests current customers from server.
     */
    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("customer_name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("customer_email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("customer_phone"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("number_of_guests"));

        ClientUI.chat.sendToServer(
                new Request("GET_CURRENT_CUSTOMERS", null)
        );
    }

    /**
     * Updates the table with current customers.
     *
     * @param list list of current customers
     */
    public void setCustomers(List<Order> list) {
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
     * Goes back to the home screen based on user role.
     *
     * @param event button click event
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            String target = Navigation.getHomeByRole();
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
}