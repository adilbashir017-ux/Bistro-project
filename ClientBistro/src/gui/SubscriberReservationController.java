package gui;

import client.ClientController;
import client.ClientUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import logic.Order;
import logic.Request;
import logic.SpecialDayDTO;
import logic.Subscriber;
import java.time.LocalDateTime;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JavaFX controller for subscriber reservations in the Terminal interface.
 * <p>
 * Supports creating reservations, cancelling by confirmation code, retrieving lost confirmation codes,
 * and performing check-in (including selecting from active orders). Also integrates with special-days
 * data and opening-hours data to control date availability and time-slot population.
 * </p>
 * <p>
 * Server communication is done via sending {@link Request} messages through {@link ClientUI#chat}.
 * The server is expected to call back into this controller using methods such as
 * {@link #updateTimes(String)}, {@link #loadSpecialDays(List)}, {@link #populateOrders(List)},
 * {@link #showSuggestedTimes(List)}, {@link #handleCheckInSuccess(int, int)},
 * {@link #handleCheckInWait()}, {@link #handleCheckInError(String)},
 * {@link #applyUpdatedEmail(String)}, and {@link #applyUpdatedPhone(String)}.
 * </p>
 */
public class SubscriberReservationController {

    /**
     * Client controller used for direct sending in some flows (e.g., requesting hours).
     */
    private ClientController client;

    /**
     * Tracks days that should be disabled due to no available reservation slots.
     */
    private final Map<LocalDate, Boolean> closedDays = new HashMap<>();

    /**
     * Stores special-day definitions keyed by date.
     */
    private final Map<LocalDate, SpecialDayDTO> specialMap = new HashMap<>();

    /** UI labels showing subscriber details and selected guest count. */
    @FXML private Label lblWelcome, lblGuests, lblName, lblEmail, lblPhone, lblSubscriberId;

    /** Date picker used for selecting a reservation date. */
    @FXML private DatePicker datePicker;

    /** Combo box containing available time slots for the selected date. */
    @FXML private ComboBox<String> timeBox;

    /** Text field used to enter (or auto-fill) the confirmation code. */
    @FXML private TextField txtConfirmationCode;

    /**
     * Combo box listing the subscriber's active reservations (for quick selection of a confirmation code).
     */
    @FXML private ComboBox<String> cbMyOrders;

    /**
     * Selected number of guests for the reservation.
     */
    private int guests = 2;

    /**
     * Current logged-in subscriber.
     */
    private Subscriber currentSubscriber;

    /**
     * Holds an order pending availability check / creation on the server side.
     */
    private Order pendingOrder;

    /**
     * Static reference to the active controller instance.
     */
    private static SubscriberReservationController ACTIVE;

    /**
     * Formatter used for displaying {@link LocalTime} values as HH:mm.
     */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Sets the client controller and triggers special-days loading for a forward-looking range.
     *
     * @param client the client controller used to send requests to the server
     */
    public void setClient(ClientController client) {
        this.client = client;
        datePicker.setValue(LocalDate.now());

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(2);
        client.sendToServer(new Request("GET_SPECIALDAYS_RANGE", List.of(from, to)));

    }

    /**
     * Initializes the controller after FXML loading.
     * <p>
     * Sets initial guest count, disables manual typing in the date picker, and registers a listener
     * to request opening hours when the date changes. Also triggers an initial hours request for today.
     * </p>
     */
    @FXML
    public void initialize() {
        ACTIVE = this;

        lblGuests.setText(String.valueOf(guests));

        datePicker.setEditable(false);

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            timeBox.getItems().clear();

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
     * Returns the active controller instance.
     *
     * @return the active controller, or {@code null} if not initialized yet
     */
    public static SubscriberReservationController getActive() {
        return ACTIVE;
    }

    /**
     * Sets the current subscriber, updates subscriber UI fields, and requests the subscriber's active orders
     * to populate {@link #cbMyOrders}.
     *
     * @param sub the logged-in subscriber
     */
    public void setSubscriber(Subscriber sub) {
        this.currentSubscriber = sub;

        lblWelcome.setText("Welcome back, " + sub.getUsername());
        lblName.setText(sub.getUsername());
        lblEmail.setText(sub.getEmail());
        lblPhone.setText(sub.getPhone());
        lblSubscriberId.setText(String.valueOf(currentSubscriber.getSubscriberId()));

        ClientUI.chat.sendToServer(
                new Request("GET_ACTIVE_ORDERS", currentSubscriber.getSubscriberId())
        );
    }

    /**
     * Loads special-day definitions and refreshes date-picker cell rendering.
     *
     * @param list list of special days received from the server
     */
    public void loadSpecialDays(List<SpecialDayDTO> list) {
        specialMap.clear();
        for (SpecialDayDTO sd : list) {
            specialMap.put(sd.date, sd);
        }
        updateDatePickerCells();
    }

    /**
     * Refreshes the date picker day-cell rendering to disable invalid days.
     * <p>
     * Disables past dates and disables days marked as closed in {@link #closedDays}.
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

                if (closedDays.getOrDefault(date, false)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #000000; -fx-text-fill: white;");
                    setTooltip(new Tooltip("Restaurant closed / no available slots"));
                } else {
                    setDisable(false);
                    setStyle(null);
                    setTooltip(null);
                }
            }
        });
    }

    /**
     * Handles date selection logic by applying special-day rules first (all-day closed / special hours),
     * otherwise requests regular hours from the server.
     * <p>
     * Note: This method is not wired in the shown code, but documents the intended behavior.
     * </p>
     *
     * @param date selected date
     */
    private void onDateSelected(LocalDate date) {
        timeBox.getItems().clear();

        SpecialDayDTO sd = specialMap.get(date);
        if (sd != null && sd.allDayClose) {
            showInfo("Restaurant is closed: " + (sd.reason != null ? sd.reason : ""));
            datePicker.setValue(null);
            return;
        }

        if (sd != null && sd.start != null && sd.end != null) {
            List<String> slots = buildSlotsFromSpecialDay(sd);
            if (!slots.isEmpty()) {
                timeBox.getItems().setAll(slots);
                timeBox.setValue(slots.get(0));
                return;
            }
        }

        if (client != null) {
            client.sendToServer(new Request("GET_HOURS", date));
        }
    }

    /**
     * Builds 30-minute time slots from a special-day time window.
     *
     * @param sd special-day definition containing start and end times
     * @return list of formatted time slots (HH:mm)
     */
    private List<String> buildSlotsFromSpecialDay(SpecialDayDTO sd) {
        List<String> slots = new ArrayList<>();
        LocalTime t = sd.start;
        while (!t.isAfter(sd.end.minusMinutes(30))) {
            slots.add(t.format(DateTimeFormatter.ofPattern("HH:mm")));
            t = t.plusMinutes(30);
        }
        return slots;
    }

    /**
     * Populates the active reservations combo box with server-provided values.
     *
     * @param orders list of display strings for active reservations
     */
    public void populateOrders(List<String> orders) {
        Platform.runLater(() -> {
            cbMyOrders.getItems().clear();
            cbMyOrders.getItems().addAll(orders);
        });
    }

    /**
     * Increases guest count up to the maximum limit and updates the UI label.
     */
    @FXML
    private void incGuests() {
        if (guests < 10) guests++;
        lblGuests.setText(String.valueOf(guests));
    }

    /**
     * Decreases guest count down to the minimum limit and updates the UI label.
     */
    @FXML
    private void decGuests() {
        if (guests > 1) guests--;
        lblGuests.setText(String.valueOf(guests));
    }

    /**
     * Validates reservation inputs and sends an availability-check request to the server.
     * <p>
     * Enforces time constraints (at least 1 hour in advance and within 1 month ahead).
     * On success, stores a pending {@link Order} and sends {@code CHECK_AVAILABILITY}.
     * </p>
     */
    @FXML
    private void onReserve() {
        if (currentSubscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        if (datePicker.getValue() == null || timeBox.getValue() == null || timeBox.getValue().isBlank()) {
            showError("Please select date and time.");
            return;
        }

        LocalDateTime selectedDateTime =
                LocalDateTime.of(datePicker.getValue(), LocalTime.parse(timeBox.getValue()));
        LocalDateTime now = LocalDateTime.now();

        if (selectedDateTime.isBefore(now.plusHours(1))) {
            showError("Reservations must be made at least 1 hour in advance.");
            return;
        }

        if (selectedDateTime.isAfter(now.plusMonths(1))) {
            showError("Reservations can be made up to 1 month ahead only.");
            return;
        }

        Order order = new Order(
                0,
                datePicker.getValue().toString(),
                timeBox.getValue(),
                guests,
                0,
                currentSubscriber.getSubscriberId(),
                currentSubscriber.getUsername(),
                currentSubscriber.getPhone(),
                currentSubscriber.getEmail(),
                LocalDate.now().toString(),
                0,
                "BOOKED",
                null,
                false,
                null
        );

        pendingOrder = order;

        ClientUI.chat.sendToServer(
                new Request("CHECK_AVAILABILITY", order)
        );
    }

    /**
     * Sends a create-reservation request to the server for the current pending order.
     * Displays an error if no pending order exists.
     */
    public void sendCreateReservation() {
        if (pendingOrder == null) {
            showError("No pending order found. Please click Reserve again.");
            return;
        }

        ClientUI.chat.sendToServer(
                new Request("CREATE_RESERVATION", pendingOrder)
        );
    }

    /**
     * Cancels a reservation by confirmation code after validation and user confirmation.
     * Sends {@code CANCEL_ORDER} to the server.
     */
    @FXML
    private void onCancelReservation() {
        if (txtConfirmationCode.getText().isBlank()) {
            showError("Please enter confirmation code.");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(txtConfirmationCode.getText().trim());
        } catch (NumberFormatException e) {
            showError("Confirmation code must be numbers only.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Reservation");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will cancel your reservation.\nConfirmation Code: " + code);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ClientUI.chat.sendToServer(
                new Request("CANCEL_ORDER", code)
        );
    }

    /**
     * Displays suggested alternative reservation times to the user.
     *
     * @param times list of suggested times received from the server
     */
    public void showSuggestedTimes(List<LocalTime> times) {
        Platform.runLater(() -> {
            if (times == null || times.isEmpty()) {
                showInfo("Sorry, no tables are available at your selected time.");
                return;
            }
            String timesStr = times.stream()
                                   .map(t -> t.format(timeFormatter))
                                   .collect(Collectors.joining(", "));
            showInfo("No tables available at your selected time.\nSuggested times:\n" + timesStr);
        });
    }

    /**
     * Performs check-in using a confirmation code.
     * <p>
     * If the text field is empty, the method attempts to extract the code from the selected active-order
     * entry in {@link #cbMyOrders}. On success, sends {@code TERMINAL_CHECKIN} to the server.
     * </p>
     */
    @FXML
    private void onCheckIn() {
        String codeStr = txtConfirmationCode.getText().trim();

        if (codeStr.isBlank()) {
            String selected = cbMyOrders.getValue();
            if (selected != null && !selected.isBlank()) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("Code:\\s*(\\d+)").matcher(selected);
                if (m.find()) {
                    codeStr = m.group(1);
                    txtConfirmationCode.setText(codeStr);
                }
            }
        }

        if (codeStr.isBlank()) {
            showError("Please enter or select a confirmation code.");
            return;
        }

        try {
            int code = Integer.parseInt(codeStr);
            ClientUI.chat.sendToServer(
                    new Request("TERMINAL_CHECKIN", code)
            );
        } catch (NumberFormatException e) {
            showError("Confirmation code must be numbers only.");
        }
    }

    /**
     * Requests the subscriber's lost confirmation code from the server.
     */
    @FXML
    private void onForgotConfirmationCode() {
        if (currentSubscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        ClientUI.chat.sendToServer(
                new Request("LOST_CONFIRMATION_CODE", currentSubscriber.getSubscriberId())
        );
    }

    /**
     * Server callback: indicates check-in succeeded and the subscriber can proceed to the given table.
     *
     * @param tableNum assigned table number
     * @param code confirmation code used for check-in
     */
    public void handleCheckInSuccess(int tableNum, int code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Check-In Successful");
        alert.setHeaderText("Welcome! Your table is ready ✅");
        alert.setContentText("Confirmation Code: " + code + "\nPlease go to Table #" + tableNum + ".\nEnjoy your meal!");
        alert.showAndWait();

        resetCheckInInput();
    }

    /**
     * Server callback: indicates there is no table available yet for check-in.
     */
    public void handleCheckInWait() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Table Available Yet");
        alert.setHeaderText("Please wait ⏳");
        alert.setContentText("All matching tables are currently occupied.\nWe will notify you as soon as a table is free.");
        alert.showAndWait();
        resetCheckInInput();
    }

    /**
     * Server callback: indicates check-in failed.
     *
     * @param msg error message to display
     */
    public void handleCheckInError(String msg) {
        showError(msg);
        resetCheckInInput();
    }

    /**
     * Navigates back to the subscriber main screen and restores subscriber context.
     */
    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberMain.fxml"));
            Parent root = loader.load();
            SubscriberMainController ctrl = loader.getController();
            ctrl.setClient(ClientUI.chat);
            ctrl.setSubscriber(currentSubscriber);
            Stage stage = (Stage) lblGuests.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a dialog to edit subscriber phone number and sends an update request to the server.
     */
    @FXML
    private void onEditPhone() {
        TextInputDialog dialog = new TextInputDialog(lblPhone.getText());
        dialog.setTitle("Edit Phone");
        dialog.setHeaderText("Change your phone number");
        dialog.setContentText("New phone:");
        dialog.showAndWait().ifPresent(newPhone -> {
            if (!newPhone.matches("\\d{9,10}")) { showError("Invalid phone number."); return; }
            ClientUI.chat.sendToServer(
                    new Request("UPDATE_SUBSCRIBER_PHONE",
                            Map.of("subscriberId", currentSubscriber.getSubscriberId(), "phone", newPhone))
            );
        });
    }

    /**
     * Opens a dialog to edit subscriber email and sends an update request to the server.
     */
    @FXML
    private void onEditEmail() {
        TextInputDialog dialog = new TextInputDialog(lblEmail.getText());
        dialog.setTitle("Edit Email");
        dialog.setHeaderText("Change your email");
        dialog.setContentText("New email:");
        dialog.showAndWait().ifPresent(newEmail -> {
            if (!newEmail.contains("@")) { showError("Invalid email."); return; }
            ClientUI.chat.sendToServer(
                    new Request("UPDATE_SUBSCRIBER_EMAIL",
                            Map.of("subscriberId", currentSubscriber.getSubscriberId(), "email", newEmail))
            );
        });
    }

    /**
     * Resets confirmation-code related inputs after check-in flow completes.
     */
    private void resetCheckInInput() {
        txtConfirmationCode.clear();
        cbMyOrders.getSelectionModel().clearSelection();
        txtConfirmationCode.requestFocus();
    }

    /**
     * Displays an error alert with the given message.
     *
     * @param msg message to display
     */
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }

    /**
     * Displays an information alert with the given message.
     *
     * @param msg message to display
     */
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }

    /**
     * Placeholder refresh handler (currently empty).
     */
    @FXML
    private void refresh() { }

    /**
     * Exits the application immediately.
     */
    @FXML
    private void exit() { System.exit(0); }

    /**
     * Navigates back to the subscriber main screen (alternate handler receiving an event).
     *
     * @param event action event triggered by the back button
     */
    @FXML
    private void back(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberMain.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates available time slots based on opening-hours information received from the server.
     * <p>
     * The input is expected to contain opening and closing time values (typically {@code "HH:mm HH:mm"}).
     * If no hours are available, the selected day is marked closed and disabled in the date picker.
     * </p>
     *
     * @param openClose opening and closing time information for the selected date
     */
    public void updateTimes(String openClose) {
        Platform.runLater(() -> {
            LocalDate date = datePicker.getValue();
            timeBox.getItems().clear();

            if (openClose == null || openClose.isBlank()) {
                closedDays.put(date, true);

                datePicker.setValue(null);

                updateDatePickerCells();

                showInfo("Selected day has no available reservation slots.");
                return;
            } else {
                closedDays.put(date, false);
            }

            String[] parts = openClose.split(" ");
            LocalTime open = LocalTime.parse(parts[0]);
            LocalTime close = LocalTime.parse(parts[1]);

            LocalTime t = open;
            while (!t.isAfter(close.minusMinutes(30))) {
                timeBox.getItems().add(t.format(DateTimeFormatter.ofPattern("HH:mm")));
                t = t.plusMinutes(30);
            }

            if (!timeBox.getItems().isEmpty()) timeBox.setValue(timeBox.getItems().get(0));

            updateDatePickerCells();
        });
    }

    /**
     * Applies an updated email value to the UI and local subscriber object.
     *
     * @param newEmail updated email value returned from the server
     */
    public void applyUpdatedEmail(String newEmail) {
        lblEmail.setText(newEmail);
        currentSubscriber.setEmail(newEmail);
    }

    /**
     * Applies an updated phone value to the UI and local subscriber object.
     *
     * @param newPhone updated phone value returned from the server
     */
    public void applyUpdatedPhone(String newPhone) {
        lblPhone.setText(newPhone);
        currentSubscriber.setPhone(newPhone);
    }

    /**
     * Updates the email label text only.
     *
     * @param newEmail email text to display
     */
    public void updateEmailLabel(String newEmail) { lblEmail.setText(newEmail); }

    /**
     * Updates the phone label text only.
     *
     * @param newPhone phone text to display
     */
    public void updatePhoneLabel(String newPhone) { lblPhone.setText(newPhone); }

    /**
     * Displays a status message using an information alert.
     *
     * @param msg message to display
     * @param color unused UI color indicator (kept for compatibility with existing calls)
     */
    public void codeStatus(String msg, String color) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }
}
