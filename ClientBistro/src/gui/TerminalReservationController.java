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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import client.ClientController;
import client.ClientUI;

/**
 * JavaFX controller for the Terminal reservation workflow.
 * <p>
 * This controller enables guests to create reservations via the Terminal interface,
 * check existing reservations by confirmation code, cancel reservations, leave the waiting list,
 * perform check-in, and trigger payment flow actions. It also supports special days handling
 * (e.g., full-day closures) by disabling dates in the {@link DatePicker}.
 * </p>
 * <p>
 * The controller communicates with the server through {@link Request} objects sent via
 * the client communication layer. Server callbacks are typically delivered through the client
 * networking layer and then routed to methods such as {@link #updateTimes(String)},
 * {@link #handleFreeTables(int)}, {@link #codeSuccess(Order)}, and {@link #codeStatus(String, String)}.
 * </p>
 */
public class TerminalReservationController implements ReservationHandler {

    /**
     * Client controller used to send requests to the server.
     */
    private ClientController client;

    /**
     * Holds the order currently being processed (e.g., before final insertion).
     */
    public Order pendingOrder;

    /**
     * Stores the confirmation code of the currently selected order for actions such as cancel,
     * payment, check-in, or leaving the waiting list.
     */
    public int codeForCancel = 0;

    /** UI labels for guest count and status messages. */
    @FXML
    private Label guestCountLabel, codeStatusLabel, orderStatusLabel, lostCode;

    /** Container showing order details after confirmation code lookup. */
    @FXML
    private VBox orderDetailsBox;

    /** Container holding suggested time buttons when alternative times are offered. */
    @FXML
    private HBox suggestedTimesBox;

    /** Date picker used for selecting reservation date. */
    @FXML
    private DatePicker datePicker;

    /** Combo box used for selecting reservation time. */
    @FXML
    private ComboBox<String> timeSelect;

    /** UI buttons used in the reservation workflow and navigation. */
    @FXML
    private Button minusGuestBtn, plusGuestBtn, orderBtn, joinBtn, checkReservationBtn, refreshBtn, backBtn, exitBtn;

    /** Input fields for customer information and confirmation code lookup. */
    @FXML
    private TextField nameField, phoneField, emailField, confirmationCodeField;

    /**
     * Current guest count selected by the user.
     */
    private int guests = 2;

    /**
     * Static instance reference.
     */
    private static TerminalReservationController instance;

    /**
     * Static reference to the currently active controller instance.
     */
    private static TerminalReservationController active;

    /**
     * Returns the active controller instance.
     *
     * @return the active controller, or {@code null} if not initialized yet
     */
    public static TerminalReservationController getActive() {
        return active;
    }

    /**
     * Tracks days that are considered closed (used for date styling/logic).
     */
    private final Map<LocalDate, Boolean> closedDays = new HashMap<>();

    /**
     * Maps special dates to their corresponding special-day configuration.
     */
    private final Map<LocalDate, SpecialDayDTO> specialMap = new HashMap<>();

    /**
     * Constructs the controller and stores a static instance reference.
     */
    public TerminalReservationController() {
        instance = this;
    }

    /**
     * Returns the current controller instance.
     *
     * @return the controller instance, or {@code null} if not initialized yet
     */
    public static TerminalReservationController getInstance() {
        return instance;
    }

    /**
     * Sets the client controller used for server communication and initializes the selected date.
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
     * Sets initial UI values, prevents manual typing in the date picker, and registers
     * a listener that requests opening hours for the selected date from the server.
     * Also triggers an initial request for today's opening hours after the UI is ready.
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
     * Updates the available reservation times based on opening and closing hours received from the server.
     * <p>
     * The input is expected to contain an opening and closing time, typically in the form:
     * {@code "HH:mm HH:mm"}. The method generates time slots in 30-minute increments and updates the UI.
     * </p>
     *
     * @param openClose a string containing opening and closing time information for the selected date
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
     * Loads special-day configurations and refreshes the date picker cell rendering.
     *
     * @param list list of special-day definitions received from the server
     */
    public void loadSpecialDays(List<SpecialDayDTO> list) {
        specialMap.clear();
        for (SpecialDayDTO sd : list) {
            specialMap.put(sd.date, sd);
        }
        updateDatePickerCells();
    }

    /**
     * Updates the {@link DatePicker} cell factory to disable invalid dates and apply special-day rules.
     * <p>
     * Disables past dates and disables full-day closures based on {@link SpecialDayDTO} configuration.
     * </p>
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
                    orderFail("Sorry but we are closed on " + date.toString(), null);
                    setDisable(true);
                    timeSelect.setDisable(true);
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
     * Increases the guest count (up to a maximum limit) and updates the UI label.
     */
    @FXML
    private void incGuests() {
        if (guests < 10) guests++;
        guestCountLabel.setText(String.valueOf(guests));
    }

    /**
     * Decreases the guest count (down to a minimum limit) and updates the UI label.
     */
    @FXML
    private void decGuests() {
        if (guests > 1) guests--;
        guestCountLabel.setText(String.valueOf(guests));
    }

    /**
     * Creates a new reservation request using the entered customer details.
     * <p>
     * Performs client-side validation (date/time window, email, phone format) and sends a request
     * to the server to locate a free table.
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

        if (date == null || time == null || name == null) {
            orderFail("Please fill the details above.", null);
            return;
        }

        LocalDate today = LocalDate.now();
        LocalTime selectedTime = LocalTime.parse(time);
        LocalDateTime selectedDateTime = LocalDateTime.of(date, selectedTime);

        if (date.isBefore(today)) {
            orderFail("Selected date has passed.", null);
            return;
        }
        if (date.isAfter(today.plusDays(30))) {
            orderFail("Reservations can only be made up to 30 days in advance.", null);
            return;
        }
        if (selectedDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            orderFail("Reservations must be made at least 1 hour in advance.", null);
            return;
        }
        if (email == null || !(email.endsWith("@gmail.com") || email.endsWith("@outlook.com"))) {
            orderFail("Please enter a valid email.", null);
            return;
        }
        if (phone.length() != 10 || !phone.matches("\\d+") || !(phone.startsWith("050") || phone.startsWith("052") || phone.startsWith("053") || phone.startsWith("054") || phone.startsWith("055") || phone.startsWith("058"))) {
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

        Request tableRequest = new Request("FREE_TABLES", order);
        client.sendToServer(tableRequest);

        pendingOrder = order;
    }

    /**
     * Creates a reservation request using the entered customer details, specifically for the terminal join flow.
     * Sends a server request to locate a free table using a terminal-specific request type.
     */
    public void handleJoin() {
        Random r = new Random();
        int confirmation = r.nextInt((999999 - 100000) + 1) + 100000;

        LocalDate date = datePicker.getValue();
        String time = timeSelect.getValue();

        if (date == null || time == null || time.isBlank()) {
            orderFail("Please select date and time.", null);
            return;
        }

        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();

        if (name == null) {
            orderFail("Please enter your name.", null);
            return;
        }

        if (email == null || !(email.endsWith("@gmail.com") || email.endsWith("@outlook.com"))) {
            orderFail("Please enter a valid email.", null);
            return;
        }
        if (phone.length() != 10 || !phone.matches("\\d+") ||
            !(phone.startsWith("050") || phone.startsWith("052") || phone.startsWith("053") ||
              phone.startsWith("054") || phone.startsWith("055") || phone.startsWith("058"))) {
            orderFail("Please enter a valid phone number.", null);
            return;
        }

        Order order = new Order(
                0,
                LocalDate.now().toString(),
                LocalTime.now().toString(),
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

        Request tableRequest = new Request("FREE_TABLES_TERMINAL", order);
        client.sendToServer(tableRequest);

        pendingOrder = order;
    }

    /**
     * Handles the server response regarding free table availability.
     * <p>
     * Updates the order state and sends insertion requests based on whether a table is available
     * or the customer should be placed on the waiting list.
     * </p>
     *
     * @param table the table number assigned by the server, or special values indicating other outcomes
     */
    public void handleFreeTables(int table) {
        if (table == 0) {
            pendingOrder.setOrder_status("WAITING");
            pendingOrder.setStatus_datetime(LocalDateTime.now().toString());
            pendingOrder.setArrival_datetime(LocalDateTime.now().toString());
            Request orderRequest = new Request("INSERT_NEW_ORDER", pendingOrder);
            client.sendToServer(orderRequest);
            orderStatusLabel.setText("No free tables currently.\nEntering waiting list.");
            orderStatusLabel.setStyle("-fx-text-fill: white;");
        } else if (table == -1) {
            Request orderRequest = new Request("INSERT_NEW_ORDER", pendingOrder);
            client.sendToServer(orderRequest);
            orderStatusLabel.setText("Order successful.\nYou should recieve a confirmation code on your email!");
            orderStatusLabel.setStyle("-fx-text-fill: green;");
            suggestedTimesBox.getChildren().clear();
            suggestedTimesBox.setVisible(false);
            suggestedTimesBox.setManaged(false);
        } else {
            pendingOrder.setOrder_status("SEATED");
            pendingOrder.setTable_num(table);
            pendingOrder.setArrival_datetime(LocalDateTime.now().toString());
            pendingOrder.setStatus_datetime(LocalDateTime.now().toString());
            Request orderRequest = new Request("INSERT_NEW_ORDER", pendingOrder);
            client.sendToServer(orderRequest);
            orderStatusLabel.setText("Welcome!\nYou can sit at table " + table);
            orderStatusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    /**
     * Displays an error message and optionally suggests alternative reservation times.
     *
     * @param msg          the message to display to the user
     * @param suggestions  optional list of suggested times
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
     * Sends a request to retrieve reservation details using the entered confirmation code.
     * Displays validation feedback if the code is invalid.
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
     * Updates the confirmation code status message and resets the order details view.
     *
     * @param msg   the message to display
     * @param color the text color to apply
     */
    public void codeStatus(String msg, String color) {
        orderDetailsBox.getChildren().clear();
        orderDetailsBox.setVisible(false);
        lostCode.setText("Lost code?");
        codeStatusLabel.setText(msg);
        codeStatusLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    /**
     * Clears user input fields and resets UI components to the default state.
     */
    @FXML
    private void refresh() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        confirmationCodeField.clear();
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
     * Displays reservation details for a valid confirmation code and presents relevant action buttons
     * based on the reservation status.
     *
     * @param oc the order returned from the server
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

        Button checkInBtn = new Button("Check In");
        checkInBtn.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 12 6 12;");
        checkInBtn.setOnAction(e -> checkIn());

        codeForCancel = order.getConfirmation_code();

        if (order.getOrder_status().equals("WAITING_SEATED") || order.getOrder_status().equals("SEATED")) {
            payBtn.setVisible(true);
            payBtn.setManaged(true);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
            exitWaitingBtn.setVisible(false);
            exitWaitingBtn.setManaged(false);
            checkInBtn.setVisible(false);
            checkInBtn.setManaged(false);
        }
        if (order.getOrder_status().equals("WAITING")) {
            exitWaitingBtn.setVisible(true);
            exitWaitingBtn.setManaged(true);
            payBtn.setVisible(false);
            payBtn.setManaged(false);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
            checkInBtn.setVisible(false);
            checkInBtn.setManaged(false);
        }
        if (order.getOrder_status().equals("BILL_SENT")) {
            exitWaitingBtn.setVisible(false);
            exitWaitingBtn.setManaged(false);
            payBtn.setVisible(true);
            payBtn.setManaged(true);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
            checkInBtn.setVisible(false);
            checkInBtn.setManaged(false);
        }
        if (order.getOrder_status().equals("WAITING_CALLED")) {
            exitWaitingBtn.setVisible(false);
            exitWaitingBtn.setManaged(false);
            payBtn.setVisible(false);
            payBtn.setManaged(false);
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
            checkInBtn.setVisible(true);
            checkInBtn.setManaged(true);

        }
        if (order.getOrder_status().equals("BOOKED")) {
            exitWaitingBtn.setVisible(false);
            exitWaitingBtn.setManaged(false);
            payBtn.setVisible(false);
            payBtn.setManaged(false);
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
            LocalDate orderDate = LocalDate.parse(order.getOrder_date());
            LocalTime orderTime = LocalTime.parse(order.getOrder_time());

            if (orderDate.equals(LocalDate.now())) {
                LocalTime now = LocalTime.now();
                LocalTime windowEnd = orderTime.plusMinutes(14);

                if (!now.isBefore(orderTime) && !now.isAfter(windowEnd)) {
                    checkInBtn.setVisible(true);
                    checkInBtn.setManaged(true);
                } else {
                    checkInBtn.setVisible(false);
                    checkInBtn.setManaged(false);
                }
            } else {
                checkInBtn.setVisible(false);
                checkInBtn.setManaged(false);
            }

        }
        orderDetailsBox.getChildren().addAll(headerLabel, emailLabel, phoneLabel, guestsLabel, dateLabel, timeLabel, placedLabel, cancelBtn, payBtn, exitWaitingBtn, checkInBtn);
    }

    /**
     * Sends a cancellation request for the currently selected confirmation code.
     */
    public void cancelOrder() {
        if (codeForCancel != 0) {
            orderDetailsBox.getChildren().clear();
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
            Request r = new Request("CANCEL_ORDER", codeForCancel);
            codeStatus("Order cancelled!", "green");
            client.sendToServer(r);
        }
    }

    /**
     * Sends a request to leave the waiting list for the currently selected confirmation code.
     */
    public void exitWaiting() {
        if (codeForCancel != 0) {
            orderDetailsBox.getChildren().clear();
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
            Request r = new Request("LEAVE_WAITING_LIST", codeForCancel);
            codeStatus("Left waiting list!", "green");
            client.sendToServer(r);
        }
    }

    /**
     * Sends a payment request for the currently selected confirmation code.
     */
    public void payBill() {
        if (codeForCancel != 0) {
            orderDetailsBox.getChildren().clear();
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
            codeStatus("Thank you for visting!", "green");
            client.sendToServer(new Request("PAY_BILL", codeForCancel));
        }
    }

    /**
     * Sends a check-in request for the currently selected confirmation code.
     */
    public void checkIn() {
        if (codeForCancel != 0) {
            orderDetailsBox.getChildren().clear();
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
            client.sendToServer(new Request("CHECK_IN", codeForCancel));
        }
    }

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Displays a simple "lost code" flow allowing the user to request their confirmation code by email.
     *
     * @param event the mouse event triggered by clicking the "Lost code?" label
     */
    @FXML
    private void lostCode(MouseEvent event) {
        lostCode.setDisable(true);

        codeStatusLabel.setText("");
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
     * Navigates back to the terminal role selection screen.
     *
     * @param event the UI action event triggered by clicking the Back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RoleSelectionTerminal.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
