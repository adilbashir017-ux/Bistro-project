package logic;

import java.io.Serializable;

/**
 * Represents opening hours for a specific day.
 * <p>
 * This class stores the day of the week along with opening and closing times.
 * It is commonly used to represent business or service operating hours.
 * </p>
 */
public class OpenHours implements Serializable{

    private int day;
    private String open,close;

    /**
     * Constructs an {@code OpenHours} object.
     * Initializes the opening and closing hours for a specific day.
     *
     * @param day   the day of the week (e.g., 1 = Sunday, 7 = Saturday)
     * @param open  the opening time
     * @param close the closing time
     */
    public OpenHours(int day, String open, String close) {
        super();
        this.day = day;
        this.open = open;
        this.close = close;
    }
    /**
     * Returns the day of the week.
     *
     * @return the day of the week
     */
    public int getDay() {
        return day;
    }
    /**
     * Sets the day of the week.
     *
     * @param day the day of the week
     */
    public void setDay(int day) {
        this.day = day;
    }
    /**
     * Returns the opening time.
     *
     * @return the opening time
     */
    public String getOpen() {
        return open;
    }
    /**
     * Sets the opening time.
     *
     * @param open the opening time
     */
    public void setOpen(String open) {
        this.open = open;
    }
    /**
     * Returns the closing time.
     *
     * @return the closing time
     */
    public String getClose() {
        return close;
    }
    /**
     * Sets the closing time.
     *
     * @param close the closing time
     */
    public void setClose(String close) {
        this.close = close;
    }



}