package gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import logic.Order;
import logic.Request;
import logic.ReservationHandler;
import logic.SpecialDayDTO;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import client.ClientController;
import client.ClientUI;

/**
 * JavaFX controller for the reservation screen.
 * <p>
 * Provides functionality for creating new reservations, selecting date/time and guest count,
 * checking existing reservations by confirmation code, and performing actions such as canceling,
 * paying, or leaving the waiting list. The controller communicates with the server by sending
 * {@link logic.Request} messages via the client communication layer.
 * </p>
 * <p>
 * The controller also supports special operating days (e.g., full-day closures) and updates
 * the {@link javafx.scene.control.DatePicker} to disable unavailable dates accordingly.
 * </p>
 */
public class ReservationController implements ReservationHandler {

    /**
     * Client controller used to send requests to the server.
     */
    private ClientController client;

    /**
     * Pending order created by the user while the system checks availability (e.g., free tables).
     */
    public Order pendingOrder;

    /**
     * Confirmation code stored for operations that act on an existing reservation
     * (e.g., cancel, pay, leave waiting list).
     */
    public int codeForCancel = 0;

    /** Label displaying the current guest count. */
    @FXML
    private Label guestCountLabel;

    /** Label displaying confirmation-code related feedback. */
    @FXML
    private Label codeStatusLabel;

    /** Label displaying reservation/order status feedback. */
    @FXML
    private Label orderStatusLabel;

    /** Label/button text used as an entry point for the "lost code" flow. */
    @FXML
    private Label lostCode;

    /** Container used to display reservation details for an existing order. */
    @FXML
    private VBox orderDetailsBox;

    /** Container used to display suggested alternative reservation times (if provided). */
    @FXML
    private HBox suggestedTimesBox;

    /** Buttons controlling guest count, ordering, navigation, refresh, and exit actions. */
    @FXML
    private Button minusGuestBtn, plusGuestBtn, orderBtn, subscriberBtn, checkReservationBtn, backBtn, refreshBtn, exitBtn;

    /** Date picker for selecting the reservation date. */
    @FXML
    private DatePicker datePicker;

    /** Combo box for selecting the reservation time. */
    @FXML
    private ComboBox<String> timeSelect;

    /** Input fields for guest user details and confirmation code lookup. */
    @FXML
    private TextField nameField, phoneField, emailField, confirmationCodeField;

    /**
     * Current guest count for a new reservation.
     */
    private int guests = 2;

    /**
     * Static reference to the current controller instance.
     * Used for accessing the controller instance from other parts of the application.
     */
    private static ReservationController instance;

    /**
     * Cache of closed/open status per date (if needed by the UI logic).
     */
    private final Map<LocalDate, Boolean> closedDays = new HashMap<>();

    /**
     * Map of special-day information keyed by date (e.g., closures or limited hours).
     */
    private final Map<LocalDate, SpecialDayDTO> specialMap = new HashMap<>();

    /**
     * Creates a new {@link ReservationController} instance and stores it as the current instance.
     */
    public ReservationController() {
        instance = this;
    }

    /**
     * Returns the current {@link ReservationController} instance.
     *
     * @return the current controller instance, or {@code null} if not initialized yet
     */
    public static ReservationController getInstance() {
        return instance;
    }

    /**
     * Sets the {@link client.ClientController} reference used for server communication.
     * Also sets the date picker to the current date.
     *
     * @param client the client controller used to send requests to the server
     */
    public void setClient(ClientController client) {
        this.client = client;
        datePicker.setValue(LocalDate.now());

    }

    /**
     * Initializes the controller after the FXML has been loaded.
     * <p>
     * Sets the initial UI state, configures the date picker behavior, and requests opening
     * hours for the current date from the server.
     * </p>
     */
    @FXML
    public void initialize() {
        instance = this;

        guestCountLabel.setText(String.valueOf(guests));

        datePicker.setEditable(false);

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            timeSelect.getItems().clear();

            if (newDate != null) {
                ClientUI.chat.sendToServer(new Request("GET_HOURS", newDate));
            }
        });

        Platform.runLater(() -> {
            LocalDate today = LocalDate.now();
            datePicker.setValue(today);
            ClientUI.chat.sendToServer(new Request("GET_HOURS", today));
        });
    }

    /**
     * Updates the available time slots based on an open/close time string returned from the server.
     * <p>
     * The expected format is two times separated by a space (e.g., {@code "10:00 22:00"}).
     * If the input is empty or blank, the selected day is treated as having no available slots.
     * </p>
     *
     * @param openClose a string containing opening and closing times for the selected date
     */
    public void updateTimes(String openClose) {
        Platform.runLater(() -> {
            LocalDate date = datePicker.getValue();
            timeSelect.getItems().clear();

            if (openClose == null || openClose.isBlank()) {

                updateDatePickerCells();

                return;
            } else {
                closedDays.put(date, false);
            }

            String[] parts = openClose.split(" ");
            LocalTime open = LocalTime.parse(parts[0]);
            LocalTime close = LocalTime.parse(parts[1]);

            LocalTime t = open;
            while (!t.isAfter(close.minusMinutes(30))) {
                timeSelect.getItems().add(t.format(DateTimeFormatter.ofPattern("HH:mm")));
                t = t.plusMinutes(30);
            }

            if (!timeSelect.getItems().isEmpty()) timeSelect.setValue(timeSelect.getItems().get(0));

            updateDatePickerCells();
        });
    }

    /**
     * Loads special-day definitions into the controller and refreshes the date picker.
     * <p>
     * Special days can represent closures or exceptions to regular opening hours.
     * </p>
     *
     * @param list list of special-day definitions returned from the server
     */
    public void loadSpecialDays(List<SpecialDayDTO> list) {
        specialMap.clear();
        for (SpecialDayDTO sd : list) {
            specialMap.put(sd.date, sd);
        }
        updateDatePickerCells();
    }

    /**
     * Updates the {@link DatePicker} cell factory to disable unavailable dates
     * and apply tooltips/styling for special days (e.g., full-day closure).
     */
    private void updateDatePickerCells() {
        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) return;

                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #dddddd;");
                    return;
                }

                SpecialDayDTO sd = specialMap.get(date);
                if (sd != null && sd.allDayClose) {
                    orderFail("Sorry but we are closed on "+date.toString(),null);
                    setDisable(true);
                    setStyle("-fx-background-color: black; -fx-text-fill: white;");
                    setTooltip(new Tooltip("Closed: " + sd.reason));
                    return;
                }

                setDisable(false);
                setStyle(null);
                setTooltip(null);
            }
        });
    }

    /**
     * Increases the guest count for the reservation (up to a defined maximum).
     */
    @FXML
    private void incGuests() {
        if (guests < 10) guests++;
        guestCountLabel.setText(String.valueOf(guests));
    }

    /**
     * Decreases the guest count for the reservation (down to a defined minimum).
     */
    @FXML
    private void decGuests() {
        if (guests > 1) guests--;
        guestCountLabel.setText(String.valueOf(guests));
    }

    /**
     * Handles creating a new reservation request based on the user input.
     * <p>
     * Validates the entered reservation details and builds an {@link Order} object.
     * Then sends a request to the server to check for available tables.
     * </p>
     */
    @FXML
    private void handleOrder() {
        Random r = new Random();
        int confirmation = r.nextInt((999999 - 100000) + 1) + 100000;

        LocalDate date = datePicker.getValue();
        String time = timeSelect.getValue();
        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();

        if (date == null || time == null || name == null || name.isBlank()) {
            orderFail("Please fill all required details.", null);
            return;
        }

        LocalDateTime selectedDateTime = LocalDateTime.of(date, LocalTime.parse(time));
        LocalDateTime now = LocalDateTime.now();

        if (selectedDateTime.isBefore(now.plusHours(1))) {
            orderFail("Reservations must be made at least 1 hour in advance.", null);
            return;
        }

        if (selectedDateTime.isAfter(now.plusMonths(1))) {
            orderFail("Reservations can be made up to 1 month in advance.", null);
            return;
        }

        if (email == null || !(email.endsWith("@gmail.com") || email.endsWith("@outlook.com"))) {
            orderFail("Please enter a valid email.", null);
            return;
        }

        if (phone.length() != 10 || !phone.matches("\\d+")
                || !(phone.startsWith("050") || phone.startsWith("052") || phone.startsWith("053")
                || phone.startsWith("054") || phone.startsWith("055") || phone.startsWith("058"))) {
            orderFail("Please enter a valid phone number.", null);
            return;
        }

        Order order = new Order(
                0,
                date.toString(),
                time,
                guests,
                confirmation,
                0,
                name,
                phone,
                email,
                LocalDateTime.now().toString(),
                0,
                "BOOKED",
                LocalDateTime.now().toString(),
                false,
                null
        );

        pendingOrder = order;

        Request tableRequest = new Request("FREE_TABLES", order);
        client.sendToServer(tableRequest);
    }

    /**
     * Handles the server response for the "free tables" request.
     * <p>
     * Updates the {@link #pendingOrder} status based on the returned table value and sends
     * an insertion request to the server.
     * </p>
     *
     * @param table the table number assigned by the server, or a special value indicating another flow
     */
    public void handleFreeTables(int table) {
        if (pendingOrder == null) return;

        if (table == 0) {
            pendingOrder.setOrder_status("WAITING");
            pendingOrder.setStatus_datetime(LocalDateTime.now().toString());

            client.sendToServer(new Request("INSERT_NEW_ORDER", pendingOrder));

            orderStatusLabel.setText("No free tables currently. Entering waiting list.");
            orderStatusLabel.setStyle("-fx-text-fill: white;");
        } else if (table == -1) {
            client.sendToServer(new Request("INSERT_NEW_ORDER", pendingOrder));

            orderStatusLabel.setText("Order successful! Your reservation has been confirmed.");
            orderStatusLabel.setStyle("-fx-text-fill: green;");

            suggestedTimesBox.getChildren().clear();
            suggestedTimesBox.setVisible(false);
            suggestedTimesBox.setManaged(false);
        } else {
            pendingOrder.setOrder_status("SEATED");
            pendingOrder.setTable_num(table);
            pendingOrder.setStatus_datetime(LocalDateTime.now().toString());

            client.sendToServer(new Request("INSERT_NEW_ORDER", pendingOrder));

            orderStatusLabel.setText("Welcome! You can sit at table " + table);
            orderStatusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    /**
     * Displays an order failure message and optionally presents alternative time suggestions.
     *
     * @param msg         the failure message to display
     * @param suggestions optional list of alternative times suggested for reservation
     */
    public void orderFail(String msg, List<LocalTime> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            orderStatusLabel.setText(msg);
            orderStatusLabel.setStyle("-fx-text-fill: red;");
        } else {
            suggestedTimesBox.getChildren().clear();
            suggestedTimesBox.setVisible(true);
            suggestedTimesBox.setSpacing(10);
            suggestedTimesBox.setPadding(new Insets(10));
            suggestedTimesBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

            orderStatusLabel.setText(msg);
            orderStatusLabel.setStyle("-fx-text-fill: red;");

            for (LocalTime t : suggestions) {
                Button timeBtn = new Button(t.toString());
                timeBtn.setStyle("-fx-background-color: #1F51FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
                timeBtn.setOnAction(e -> timeSelect.setValue(timeBtn.getText()));
                suggestedTimesBox.getChildren().add(timeBtn);
            }
        }
    }

    /**
     * Sends a confirmation-code lookup request to the server based on user input.
     */
    @FXML
    private void checkCode() {
        try {
            int conCode = Integer.parseInt(confirmationCodeField.getText());
            Request r = new Request("CONFIRMATION_CODE", conCode);
            client.sendToServer(r);
        } catch (Exception e) {
            e.printStackTrace();
            codeStatus("Invalid input.", "red");
        }
    }

    /**
     * Handles a successful confirmation-code lookup by displaying the retrieved order details.
     *
     * @param oc the order returned by the server for the given confirmation code
     */
    public void codeSuccess(Order oc) {
        Order order = oc;

        lostCode.setText("");
        codeStatusLabel.setText("");

        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(true);
        orderDetailsBox.setSpacing(10);
        orderDetailsBox.setPadding(new Insets(10));
        orderDetailsBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        Label headerLabel = new Label("Welcome " + order.getCustomer_name() + "!\nHere are your order details:");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        Label emailLabel = new Label("Email: " + order.getCustomer_email());
        emailLabel.setStyle("-fx-text-fill: #555555;");

        Label phoneLabel = new Label("Phone: " + order.getCustomer_phone());
        phoneLabel.setStyle("-fx-text-fill: #555555;");

        Label guestsLabel = new Label("Guests: " + order.getNumber_of_guests());
        guestsLabel.setStyle("-fx-text-fill: #555555;");

        Label dateLabel = new Label("Date: " + order.getOrder_date());
        dateLabel.setStyle("-fx-text-fill: #555555;");

        Label timeLabel = new Label("Time: " + order.getOrder_time());
        timeLabel.setStyle("-fx-text-fill: #555555;");

        Label placedLabel = new Label("Order Placed On: " + order.getDate_of_placing_order());
        placedLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px;");

        Button cancelBtn = new Button("Cancel Order");
        cancelBtn.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
        cancelBtn.setOnAction(e -> cancelOrder());

        Button exitWaitingBtn = new Button("Exit Waiting List");
        exitWaitingBtn.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
        exitWaitingBtn.setOnAction(e -> exitWaiting());

        int amount = 50 * order.getNumber_of_guests();
        Button payBtn = new Button("Pay Bill: ₪" + amount);
        payBtn.setStyle("-fx-background-color: #1F51FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
        payBtn.setOnAction(e -> payBill());

        codeForCancel = order.getConfirmation_code();

        switch (order.getOrder_status()) {
            case "WAITING_SEATED", "SEATED" -> {
                payBtn.setVisible(true);
                payBtn.setManaged(true);
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
                exitWaitingBtn.setVisible(false);
                exitWaitingBtn.setManaged(false);
            }
            case "WAITING" -> {
                exitWaitingBtn.setVisible(true);
                exitWaitingBtn.setManaged(true);
                payBtn.setVisible(false);
                payBtn.setManaged(false);
                cancelBtn.setVisible(true);
                cancelBtn.setManaged(true);
            }
            case "BILL_SENT" -> {
                exitWaitingBtn.setVisible(false);
                exitWaitingBtn.setManaged(false);
                payBtn.setVisible(true);
                payBtn.setManaged(true);
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
            case "WAITING_CALLED", "BOOKED" -> {
                exitWaitingBtn.setVisible(false);
                exitWaitingBtn.setManaged(false);
                payBtn.setVisible(false);
                payBtn.setManaged(false);
                cancelBtn.setVisible(true);
                cancelBtn.setManaged(true);
            }
        }

        orderDetailsBox.getChildren().addAll(headerLabel, emailLabel, phoneLabel, guestsLabel, dateLabel, timeLabel, placedLabel, cancelBtn, payBtn, exitWaitingBtn);
    }

    /**
     * Displays a confirmation-code status message and resets the order-details view.
     *
     * @param msg   the status message to display
     * @param color a CSS color value used for the status text
     */
    public void codeStatus(String msg, String color) {
        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(false);
        lostCode.setText("Lost code?");
        codeStatusLabel.setText(msg);
        codeStatusLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    /**
     * Sends a cancellation request to the server for the current confirmation code.
     */
    public void cancelOrder() {
        if (codeForCancel != 0) {
            client.sendToServer(new Request("CANCEL_ORDER", codeForCancel));
        }
    }

    /**
     * Initiates bill payment for the current confirmation code and updates the UI accordingly.
     */
    public void payBill() {
        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(false);
        orderDetailsBox.setManaged(false);
        codeStatus("Thank you for visiting!", "green");
        client.sendToServer(new Request("PAY_BILL", codeForCancel));
    }

    /**
     * Sends a request to leave the waiting list for the current confirmation code
     * and updates the UI accordingly.
     */
    public void exitWaiting() {
        if (codeForCancel != 0) {
            orderDetailsBox.getChildren().clear();
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
            client.sendToServer(new Request("LEAVE_WAITING_LIST", codeForCancel));
            codeStatus("Left waiting list!", "green");
        }
    }

    /**
     * Handles the "lost code" flow by asking the user for an email address
     * and sending a request to the server to email the confirmation code.
     *
     * @param event mouse click event that triggers the lost-code flow
     */
    @FXML
    public void lostCode(MouseEvent event) {
        lostCode.setDisable(true);

        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(true);
        orderDetailsBox.setManaged(true);
        orderDetailsBox.setSpacing(10);
        orderDetailsBox.setPadding(new Insets(10));
        orderDetailsBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        Label emailLabel = new Label("Please enter your email:");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");

        Button sendBtn = new Button("Send Code");
        sendBtn.setStyle("-fx-background-color: #1F51FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
        sendBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty() || !(email.endsWith("@gmail.com") || email.endsWith("@outlook.com"))) {
                codeStatus("Please enter a valid email", "red");
                return;
            }
            client.sendToServer(new Request("SEND_CODE", email));
            codeStatus("A code was sent to your email", "green");
        });

        orderDetailsBox.getChildren().addAll(emailLabel, emailField, sendBtn);
        lostCode.setDisable(false);
    }

    /**
     * Resets all input fields and UI components to their initial state.
     */
    @FXML
    private void refresh() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        confirmationCodeField.clear();
        timeSelect.getItems().clear();
        timeSelect.setValue(null);
        datePicker.setValue(LocalDate.now());
        guestCountLabel.setText("2");

        orderStatusLabel.setText("");
        codeStatusLabel.setText("");
        lostCode.setText("Lost code?");
        lostCode.setDisable(false);

        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(false);
        orderDetailsBox.setManaged(false);

        suggestedTimesBox.getChildren().clear();
        suggestedTimesBox.setVisible(false);
        suggestedTimesBox.setManaged(false);
    }

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the application role selection screen.
     *
     * @param event the UI action event triggered by clicking the Back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RoleSelectionApp.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
