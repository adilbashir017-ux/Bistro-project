package Server;

import javafx.application.Application;
import javafx.stage.Stage;
import gui.ServerPortFrameController;

/**
 * Main server user interface class.
 * <p>
 * This class extends {@link Application} and serves as the entry point
 * for launching the server-side graphical user interface.
 * It allows the user to select a port and start the server.
 * </p>
 */
public class ServerUI extends Application {

    /**
     * Default port number used by the server.
     */
    final public static int DEFAULT_PORT = 5555;

    /**
     * Default constructor for ServerUI.
     */
    public ServerUI() {
    }


    /**
     * The main entry point of the server application.
     *
     * @param args command-line arguments passed to the application
     * @throws Exception if an error occurs during application launch
     */
    public static void main( String args[] ) throws Exception
    {
        launch(args);
    } // end main

    /**
     * Starts the JavaFX application.
     * <p>
     * Initializes and displays the server port configuration window.
     * </p>
     *
     * @param primaryStage the primary stage for this application
     * @throws Exception if an error occurs during startup
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // TODO Auto-generated method stub
        ServerPortFrameController aFrame = new ServerPortFrameController();

        aFrame.start(primaryStage);
    }

    /**
     * Starts the server on the specified port.
     *
     * @param p the port number as a string
     */
    public static void runServer(String p)
    {
        int port = 0; //Port to listen on

        try
        {
            port = Integer.parseInt(p); //Set port to 5555
        }
        catch(Throwable t)
        {
            System.out.println("ERROR - Could not connect!");
        }

        EchoServer sv = new EchoServer(port);

        try
        {
            sv.listen(); //Start listening for connections
        }
        catch (Exception ex)
        {
            System.out.println("ERROR - Could not listen for clients!");
        }
    }
}