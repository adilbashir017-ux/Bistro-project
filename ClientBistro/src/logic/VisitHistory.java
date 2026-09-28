package logic;

import java.io.Serializable;

/**
 * Represents a visit history record.
 * <p>
 * This class stores information about a past visit, including the visit date,
 * number of people, and the visit status. It is typically used as a
 * Data Transfer Object (DTO) for account or visit history views.
 * </p>
 */
public class VisitHistory implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The date of the visit */
    private String visitDate;

    /** The number of people in the visit */
    private int people;

    /** The status of the visit */
    private String status;

    /**
     * Constructs a {@code VisitHistory} object.
     * Initializes the visit details.
     *
     * @param visitDate the date of the visit
     * @param people    the number of people in the visit
     * @param status    the status of the visit
     */
    public VisitHistory(String visitDate, int people, String status) {
        this.visitDate = visitDate;
        this.people = people;
        this.status = status;
    }

    /**
     * Returns the visit date.
     *
     * @return the visit date
     */
    public String getVisitDate() {
        return visitDate;
    }

    /**
     * Returns the number of people in the visit.
     *
     * @return the number of people
     */
    public int getPeople() {
        return people;
    }

    /**
     * Returns the visit status.
     *
     * @return the visit status
     */
    public String getStatus() {
        return status;
    }
}