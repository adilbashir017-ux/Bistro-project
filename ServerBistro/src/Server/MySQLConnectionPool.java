package Server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a simple MySQL connection pool.
 * <p>
 * It manages a pool of reusable database connections in order to improve
 * performance and reduce the overhead of creating new connections.
 * The class follows the Singleton design pattern to ensure a single
 * shared pool throughout the application.
 * </p>
 */
public class MySQLConnectionPool {

    /**
     * The single instance of the {@code MySQLConnectionPool} class.
     */
    private static MySQLConnectionPool instance;

    /**
     * List of available (free) database connections.
     */
    private final List<Connection> availableConnections = new ArrayList<>();

    /**
     * List of database connections currently in use.
     */
    private final List<Connection> usedConnections = new ArrayList<>();

    /**
     * Initial number of connections created in the pool.
     */
    private static final int INITIAL_POOL_SIZE = 5;

    /**
     * Private constructor to prevent external instantiation.
     * <p>
     * Initializes the connection pool with a predefined number of
     * database connections.
     * </p>
     *
     * @throws SQLException if a database access error occurs
     */
    private MySQLConnectionPool() throws SQLException {
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            availableConnections.add(MySQLConnection.getInstance().getConnection());
        }
    }

    /**
     * Returns the singleton instance of {@code MySQLConnectionPool}.
     * <p>
     * If the instance does not exist, it will be created.
     * This method is synchronized to ensure thread safety.
     * </p>
     *
     * @return the single {@code MySQLConnectionPool} instance
     * @throws SQLException if a database access error occurs
     */
    public static synchronized MySQLConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    /**
     * Retrieves a database connection from the pool.
     * <p>
     * If no connections are available, a new connection is created
     * and added to the pool.
     * </p>
     *
     * @return a {@link Connection} object from the pool
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (availableConnections.isEmpty()) {
            // optional: create more or block
            availableConnections.add(MySQLConnection.getInstance().getConnection());
        }

        Connection conn = availableConnections.remove(0);
        usedConnections.add(conn);
        return conn;
    }

    /**
     * Releases a database connection back to the pool.
     *
     * @param conn the {@link Connection} to be returned to the pool
     */
    public synchronized void releaseConnection(Connection conn) {
        usedConnections.remove(conn);
        availableConnections.add(conn);
    }
}