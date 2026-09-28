package gui;

import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import logic.OpenHours;
import logic.Request;
import logic.SpecialDayDTO;
import navigation.Navigation;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller for managing the restaurant's opening hours and special days.
 * <p>
 * Allows the manager to set regular weekly hours or define special/holiday days
 * with optional closing or custom hours. Communicates with the server using
 * {@link ClientUI} to persist changes.
 * </p>
 * <p>
 * The UI dynamically shows or hides fields based on whether a normal weekday
 * or a special/holiday day is selected.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * OpeningHoursController controller = loader.getController();
 * // User interacts with UI and clicks Save
 * </pre>
 *
 * @author Mohamad
 * @version 1.0
 */
public class OpeningHoursController {

    /** ComboBox to select the day or "Holiday / Special Day" */
    @FXML private ComboBox<String> dayBox;

    /** TextField to input opening time for regular days */
    @FXML private TextField openField;

    /** TextField to input closing time for regular days */
    @FXML private TextField closeField;

    /** TextField to input reason for special day closure */
    @FXML private TextField reason;

    /** Label showing status messages to the user */
    @FXML private Label statusLabel;

    /** DatePicker for selecting a special day */
    @FXML private DatePicker specialDate;

    /** Buttons for navigation and special day deletion */
    @FXML private Button backBtn, exitBtn, deleteBtn;

    /**
     * Initializes the controller.
     * <p>
     * Populates the day selection ComboBox, and sets up dynamic visibility
     * of special day fields based on the selected option.
     * </p>
     */
    @FXML
    public void initialize() {
        dayBox.getItems().addAll(
            "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday",
            "Holiday / Special Day"
        );

        specialDate.setVisible(false);
        specialDate.setManaged(false);
        reason.setVisible(false);
        reason.setManaged(false);
        deleteBtn.setVisible(false);
        deleteBtn.setManaged(false);

        dayBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isHoliday = "Holiday / Special Day".equals(newVal);

            specialDate.setVisible(isHoliday);
            specialDate.setManaged(isHoliday);
            reason.setVisible(isHoliday);
            reason.setManaged(isHoliday);
            deleteBtn.setVisible(isHoliday);
            deleteBtn.setManaged(isHoliday);
        });
    }

    /**
     * Saves opening/closing hours or special day info.
     * <p>
     * Sends a {@link Request} to the server with the updated hours or
     * special day details. Handles validation for missing fields.
     * </p>
     */
    @FXML
    private void saveHours() {
        String dayStr = dayBox.getValue();
        if (dayStr == null) {
            statusLabel.setText("Please choose a day");
            return;
        }

        boolean isHoliday = dayStr.equals("Holiday / Special Day");

        LocalTime open = parseTime(openField);
        LocalTime close = parseTime(closeField);

        // Special/Holiday day
        if (isHoliday) {
            LocalDate date = specialDate.getValue();
            String txt = reason.getText();

            if (date == null) {
                statusLabel.setText("Please choose a date");
                return;
            }

            boolean closedAllDay = (open == null || close == null);

            SpecialDayDTO sd = new SpecialDayDTO(date, closedAllDay, open, close, txt);
            ClientUI.chat.sendToServer(new Request("SET_SPECIAL_DAY", sd));
            statusLabel.setText("Special day saved");
            return;
        }

        // Regular weekday
        if (open == null || close == null) {
            statusLabel.setText("Opening and closing times are required");
            return;
        }

        int day = convertDayToInt(dayStr);
        OpenHours hours = new OpenHours(day, open.toString(), close.toString());
        ClientUI.chat.sendToServer(new Request("SET_OPEN_HOURS", hours));
        statusLabel.setText("Opening & closing hours updated");
    }

    /**
     * Parses the input from a TextField into a {@link LocalTime}.
     *
     * @param field The TextField containing the time
     * @return The LocalTime value, or null if the field is empty
     */
    private LocalTime parseTime(TextField field) {
        if (field.getText() == null || field.getText().isBlank())
            return null;
        return LocalTime.parse(field.getText());
    }

    /**
     * Deletes the selected special day.
     * <p>
     * Sends a {@link Request} to the server to remove the special day.
     * </p>
     */
    @FXML
    private void deleteSpecialDay() {
        LocalDate date = specialDate.getValue();
        if (date == null) {
            statusLabel.setText("Please choose a date");
            return;
        }
        ClientUI.chat.sendToServer(new Request("DELETE_SPECIAL_DAY", date));
        statusLabel.setText("Special day deleted");
    }

    /**
     * Converts a weekday string to an integer representation (1-7).
     *
     * @param day The day string
     * @return The integer corresponding to the day
     */
    private int convertDayToInt(String day) {
        return switch (day) {
            case "Sunday" -> 1;
            case "Monday" -> 2;
            case "Tuesday" -> 3;
            case "Wednesday" -> 4;
            case "Thursday" -> 5;
            case "Friday" -> 6;
            case "Saturday" -> 7;
            default -> 0;
        };
    }

    /**
     * Exits the application.
     */
    @FXML
    private void exit() {
        System.exit(0);
    }

    /**
     * Navigates back to the home page based on role.
     *
     * @param event The ActionEvent from the back button
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}