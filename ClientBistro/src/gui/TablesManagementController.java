package gui;

import client.ClientUI;
import logic.Request;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import logic.Tables;
import navigation.Navigation;

import java.util.List;

/**
 * Controller for managing tables in the restaurant.
 * <p>
 * Provides functionality to add, update, and delete tables.
 * Displays all tables in a {@link TableView} with their ID and capacity.
 * </p>
 */
public class TablesManagementController {

    /** Reference to the active controller instance. */
    public static TablesManagementController activeController;

    /** TableView displaying all tables. */
    @FXML
    private TableView<Tables> tablesTable;

    /** Column showing the table ID. */
    @FXML
    private TableColumn<Tables, Integer> tableIdCol;

    /** Column showing the table capacity. */
    @FXML
    private TableColumn<Tables, Integer> capacityCol;

    /** TextField for entering a table's capacity. */
    @FXML
    private TextField capacityField;

    /** Label to show status messages. */
    @FXML
    private Label statusLabel;

    /** Button to go back to the previous screen. */
    @FXML
    private Button backBtn;

    /** Button to exit the application. */
    @FXML
    private Button exitBtn;

    /** Observable list storing tables to display in the TableView. */
    private final ObservableList<Tables> tableList = FXCollections.observableArrayList();

    /**
     * Initializes the controller.
     * <p>
     * Sets up TableView columns and requests the current list of tables from the server.
     * </p>
     */
    @FXML
    public void initialize() {
        activeController = this;

        tableIdCol.setCellValueFactory(new PropertyValueFactory<>("table_num"));
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("places"));

        tablesTable.setItems(tableList);

        // Request table data from the server
        ClientUI.chat.sendToServer(new Request("GET_TABLES", null));
    }

    /**
     * Populates the TableView with a list of tables.
     *
     * @param tables a {@link List} of {@link Tables} objects to display
     */
    public void setTables(List<Tables> tables) {
        tableList.clear();
        tableList.addAll(tables);
    }

    /**
     * Adds a new table with the capacity specified in {@link #capacityField}.
     * <p>
     * Sends an "ADD_TABLE" request to the server. Displays a status message if the input is invalid.
     * </p>
     */
    @FXML
    private void addTable() {
        String text = capacityField.getText().trim();

        if (text.isEmpty()) {
            statusLabel.setText("Enter capacity");
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            statusLabel.setText("Capacity must be a number");
            return;
        }

        ClientUI.chat.sendToServer(new Request("ADD_TABLE", capacity));
        capacityField.clear();
        statusLabel.setText("Adding table...");
    }

    /**
     * Updates the capacity of the selected table in the TableView.
     * <p>
     * Sends an "UPDATE_TABLE" request to the server. Validates the selection and input capacity.
     * </p>
     */

    @FXML
    private void updateTable() {
        Tables selected = tablesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a table");
            return;
        }

        int newCapacity;
        try {
            newCapacity = Integer.parseInt(capacityField.getText());
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid capacity");
            return;
        }

        selected.setPlaces(newCapacity);
        ClientUI.chat.sendToServer(new Request("UPDATE_TABLE", selected));

        // Refresh from server
        ClientUI.chat.sendToServer(new Request("GET_TABLES", null));

        statusLabel.setText("Updating table...");
    }


    /**
     * Deletes the selected table from the TableView.
     * <p>
     * Sends a "DELETE_TABLE" request to the server. Validates the selection.
     * </p>
     */
    @FXML
    private void deleteTable() {
        Tables selected = tablesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a table");
            return;
        }

        ClientUI.chat.sendToServer(new Request("DELETE_TABLE", selected.getTable_num()));
        statusLabel.setText("Deleting table...");
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
     * @param event the {@link ActionEvent} triggered by the back button
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