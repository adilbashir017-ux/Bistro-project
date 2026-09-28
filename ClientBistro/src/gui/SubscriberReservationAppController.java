package gui;

import client.ClientController;
import client.ClientUI;
import javafx.application.Platform;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JavaFX controller for subscriber reservations in the Application (App) interface.
 * <p>
 * Provides reservation creation for subscribers, cancellation by confirmation code,
 * lost confirmation-code recovery, and profile updates (email/phone). It also integrates
 * with opening-hours and special-days data to enable/disable dates and populate time slots.
 * </p>
 * <p>
 * Communication with the server is performed by sending {@link Request} objects through
 * the client communication layer. Server responses are expected to update the UI by invoking
 * callback methods such as {@link #updateTimes(String)}, {@link #loadSpecialDays(List)},
 * {@link #showSuggestedTimes(List)}, {@link #applyUpdatedEmail(String)}, and
 * {@link #applyUpdatedPhone(String)}.
 * </p>
 */
public class SubscriberReservationAppController implements SubscriberChildScreen {

    /**
     * Client controller used to send requests to the server.
     */
    private ClientController client;

    /** UI labels displaying subscriber details and selected guest count. */
    @FXML private Label lblWelcome, lblGuests, lblName, lblEmail, lblPhone, lblSubscriberId;

    /** Date picker used for reservation date selection. */
    @FXML private DatePicker datePicker;

    /** Combo box containing available reservation time slots for the selected date. */
    @FXML private ComboBox<String> timeBox;

    /** Text field used for entering confirmation code for cancellation. */
    @FXML private TextField txtConfirmationCode;

    /**
     * Tracks days that should be disabled due to no available slots.
     */
    private final Map<LocalDate, Boolean> closedDays = new HashMap<>();

    /**
     * Maps special dates to their corresponding special-day configuration.
     */
    private final Map<LocalDate, SpecialDayDTO> specialMap = new HashMap<>();

    /**
     * Current guest count selected by the subscriber.
     */
    private int guests = 2;

    /**
     * Current logged-in subscriber.
     */
    private Subscriber currentSubscriber;

    /**
     * Holds a reservation order that is pending server approval/creation.
     */
    private Order pendingOrder;

    /**
     * Static reference to the active controller instance.
     */
    private static SubscriberReservationAppController ACTIVE;

    /**
     * Formatter used for displaying time slots and suggested times.
     */
    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Sets the client controller and triggers loading of special days for a forward-looking range.
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
     * Sets initial guest count, prevents manual typing in the date picker, registers a listener to
     * request opening hours for selected dates, and triggers an initial opening-hours request for today.
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
     * Updates the available time slots based on opening-hours information received from the server.
     * <p>
     * The input is expected to contain opening and closing time values (typically {@code "HH:mm HH:mm"}).
     * If no hours are available, the selected day is marked as closed and disabled in the date picker.
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
     * Loads special-day definitions into the controller and refreshes the date picker cell rendering.
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
     * Refreshes the date picker day cells to disable invalid dates.
     * <p>
     * Disables past dates and disables dates that were marked as closed due to no available slots.
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
     * Builds a list of reservation time slots in 30-minute increments from a special-day time window.
     *
     * @param sd the special-day definition containing a start and end time window
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
     * Returns the active controller instance.
     *
     * @return the active controller, or {@code null} if not initialized yet
     */
    public static SubscriberReservationAppController getActive() {
        return ACTIVE;
    }

    /**
     * Sets the current subscriber and populates the UI labels with subscriber information.
     *
     * @param sub the logged-in subscriber
     */
    @Override
    public void setSubscriber(Subscriber sub) {
        this.currentSubscriber = sub;

        if (sub == null) return;

        lblWelcome.setText("Welcome back, " + sub.getUsername());
        lblName.setText(sub.getUsername());
        lblEmail.setText(sub.getEmail());
        lblPhone.setText(sub.getPhone());
        lblSubscriberId.setText(String.valueOf(currentSubscriber.getSubscriberId()));

    }

    /**
     * Increases the guest count (up to a maximum limit) and updates the UI label.
     */
    @FXML
    private void incGuests() {
        if (guests < 10) guests++;
        lblGuests.setText(String.valueOf(guests));
    }

    /**
     * Decreases the guest count (down to a minimum limit) and updates the UI label.
     */
    @FXML
    private void decGuests() {
        if (guests > 1) guests--;
        lblGuests.setText(String.valueOf(guests));
    }

    /**
     * Validates subscriber reservation input and sends an availability-check request to the server.
     * <p>
     * The method enforces time constraints (at least 1 hour in advance and within 1 month ahead).
     * If validation succeeds, a pending {@link Order} is created and sent to the server for availability checking.
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
     * Sends a request to create the reservation for the current pending order.
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
     * Cancels a reservation by confirmation code after validating input and obtaining user confirmation.
     * Sends a cancellation request to the server.
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
        confirm.setContentText("Confirmation Code: " + code);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        ClientUI.chat.sendToServer(
                new Request("CANCEL_ORDER", code)
        );
    }

    /**
     * Requests the subscriber's lost confirmation code from the server using subscriber ID.
     */
    @FXML
    private void onForgotConfirmationCode() {

        if (currentSubscriber == null) {
            showError("Subscriber not loaded.");
            return;
        }

        ClientUI.chat.sendToServer(
                new Request("LOST_CONFIRMATION_CODE",
                        currentSubscriber.getSubscriberId())
        );
    }

    /**
     * Displays suggested alternative reservation times to the subscriber.
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
     * Navigates back to the subscriber main screen while preserving subscriber context.
     */
    @FXML
    private void onBack() {

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/SubscriberMainApp.fxml"));
            Parent root = loader.load();

            SubscriberMainControllerApp ctrl = loader.getController();
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
     * Performs basic validation on the new phone value.
     */
    @FXML
    private void onEditPhone() {

        TextInputDialog dialog = new TextInputDialog(lblPhone.getText());
        dialog.setTitle("Edit Phone");

        dialog.showAndWait().ifPresent(newPhone -> {

            if (!newPhone.matches("\\d{9,10}")) {
                showError("Invalid phone number.");
                return;
            }

            ClientUI.chat.sendToServer(
                    new Request("UPDATE_SUBSCRIBER_PHONE",
                            Map.of("subscriberId", currentSubscriber.getSubscriberId(),
                                   "phone", newPhone))
            );
        });
    }

    /**
     * Opens a dialog to edit subscriber email and sends an update request to the server.
     * Performs basic validation on the new email value.
     */
    @FXML
    private void onEditEmail() {

        TextInputDialog dialog = new TextInputDialog(lblEmail.getText());
        dialog.setTitle("Edit Email");

        dialog.showAndWait().ifPresent(newEmail -> {

            if (!newEmail.contains("@")) {
                showError("Invalid email.");
                return;
            }

            ClientUI.chat.sendToServer(
                    new Request("UPDATE_SUBSCRIBER_EMAIL",
                            Map.of("subscriberId", currentSubscriber.getSubscriberId(),
                                   "email", newEmail))
            );
        });
    }

    /**
     * Applies an updated email value to the UI and local subscriber object.
     *
     * @param newEmail updated email returned from the server
     */
    public void applyUpdatedEmail(String newEmail) {
        lblEmail.setText(newEmail);
        currentSubscriber.setEmail(newEmail);
    }

    /**
     * Applies an updated phone value to the UI and local subscriber object.
     *
     * @param newPhone updated phone returned from the server
     */
    public void applyUpdatedPhone(String newPhone) {
        lblPhone.setText(newPhone);
        currentSubscriber.setPhone(newPhone);
    }

    /**
     * Displays an error dialog with the provided message.
     *
     * @param msg error message to display
     */
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    /**
     * Displays an information dialog with the provided message.
     *
     * @param msg information message to display
     */
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
