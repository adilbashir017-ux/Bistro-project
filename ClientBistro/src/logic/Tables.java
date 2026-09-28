package logic;

import java.io.Serializable;

/**
 * Represents a table in the system.
 * <p>
 * This class stores information about a table, including its table number
 * and the number of available places. It is typically used for reservation
 * and seating management.
 * </p>
 */
public class Tables implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The table number */
    private int table_num;

    /** The number of seats available at the table */
    private int places;

    /** The order number currently associated with the table (if any) */
    private int order_num;

    /**
     * Constructs a {@code Tables} object.
     * Initializes the table number and number of places.
     *
     * @param table_num the table number
     * @param places    the number of seats at the table
     */
    public Tables(int table_num, int places) {
        this.table_num = table_num;
        this.places = places;
    }

    /**
     * Returns the table number.
     *
     * @return the table number
     */
    public int getTable_num() {
        return table_num;
    }

    /**
     * Sets the table number.
     *
     * @param table_num the table number
     */
    public void setTable_num(int table_num) {
        this.table_num = table_num;
    }

    /**
     * Returns the number of places at the table.
     *
     * @return the number of places
     */
    public int getPlaces() {
        return places;
    }

    /**
     * Sets the number of places at the table.
     *
     * @param places the number of places
     */
    public void setPlaces(int places) {
        this.places = places;
    }

    /**
     * Returns a string representation of the table.
     *
     * @return a string representation of the table
     */
    @Override
    public String toString() {
        return "Tables [table_num=" + table_num
                + ", places=" + places
                + "]";
    }
}