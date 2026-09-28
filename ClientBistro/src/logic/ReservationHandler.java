package logic;

import java.time.LocalTime;
import java.util.List;
/**
 * Defines callback methods for handling reservation-related events.
 * <p>
 * This interface is used to notify implementing classes about
 * reservation results, status updates, and available alternatives.
 * </p>
 */
public interface ReservationHandler {
       /**
     * Handles the case where free tables are available.
     *
     * @param i the number of available free tables
     */
    void handleFreeTables(int i);
    /**
     * Handles a reservation failure.
     * <p>
     * Provides a failure message along with suggested alternative times.
     * </p>
     *
     * @param message     a message describing the failure reason
     * @param suggestions a list of suggested alternative reservation times
     */
    void orderFail(String message, List<LocalTime> suggestions);

    /**
     * Handles a successful reservation code generation.
     *
     * @param order the order associated with the successful reservation
     */
    void codeSuccess(Order order);
    /**
     * Updates the reservation status with a message and visual indicator.
     *
     * @param message a status message
     * @param color   a color representing the status (e.g., success or error)
     */
    void codeStatus(String message,String color);

    /**
     * Updates available reservation times.
     *
     * @param data serialized or formatted data representing updated times
     */
    void updateTimes(String data);
}