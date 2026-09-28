// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com

package common;

/**
 * This interface defines the abstract method used to display
 * messages or objects onto client or server user interfaces.
 * <p>
 * Any class implementing this interface must provide a concrete
 * implementation of the {@link #display(String)} method.
 * </p>
 *
 * <p>Author: Dr Robert Lagani&egrave;re, Dr Timothy C. Lethbridge</p>
 * @version July 2000
 */
public interface ChatIF
{
    /**
     * Displays a message on the UI.
     * <p>
     * Implementing classes should define how the message is presented,
     * for example printing to console, updating a text area, or logging.
     * </p>
     *
     * @param message the message or object to display
     */
    public abstract void display(String message);
}