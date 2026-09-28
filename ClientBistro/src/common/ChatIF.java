// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com

package common;

/**
 * The <code>ChatIF</code> interface defines the contract for any class that
 * wants to display messages to a user interface (UI) in a client-server
 * application.
 * <p>
 * Classes implementing this interface must provide their own version of
 * {@link #display(String)} to show messages on the client or server UI.
 * This allows the server and client to be decoupled from the actual UI
 * implementation.
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * public class ClientConsole implements ChatIF {
 *     public void display(String message) {
 *         System.out.println(message);
 *     }
 * }
 * </pre>
 *
 * @author Dr Robert Lagani&egrave;re
 * @author Dr Timothy C. Lethbridge
 * @version July 2000
 */
public interface ChatIF
{
  /**
   * Displays a message on the user interface.
   * <p>
   * Implementing classes decide how to render the message, whether
   * to a console, GUI window, or other output.
   *
   * @param message the text message to display on the UI.
   */
  public abstract void display(String message);
}