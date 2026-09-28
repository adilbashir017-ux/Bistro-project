package logic;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) representing a special day configuration.
 * <p>
 * This class is used to describe special operating conditions for a specific date,
 * such as full-day closure or limited opening hours, along with an optional reason.
 * </p>
 */
public class SpecialDayDTO implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The date of the special day */
    public LocalDate date;

    /** Indicates whether the business is closed for the entire day */
    public boolean allDayClose;

    /** The opening time on the special day (if not closed all day) */
    public LocalTime start;

    /** The closing time on the special day (if not closed all day) */
    public LocalTime end;

    /** The reason for the special day schedule (e.g., holiday, event) */
    public String reason;

    /**
     * Constructs a {@code SpecialDayDTO} object.
     * Initializes the special day details including date, opening hours,
     * closure status, and reason.
     *
     * @param date         the date of the special day
     * @param allDayClose  indicates whether the business is closed all day
     * @param start        the opening time (nullable if closed all day)
     * @param end          the closing time (nullable if closed all day)
     * @param reason       the reason for the special day schedule
     */
    public SpecialDayDTO(LocalDate date, boolean allDayClose,
                         LocalTime start, LocalTime end, String reason) {
        this.date = date;
        this.allDayClose = allDayClose;
        this.start = start;
        this.end = end;
        this.reason = reason;
    }
}