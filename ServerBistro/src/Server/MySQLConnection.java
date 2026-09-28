package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class manages connections to a MySQL database.
 * <p>
 * It follows the Singleton design pattern to ensure that the MySQL JDBC driver
 * is loaded only once and that a single shared instance is used throughout
 * the application.
 * </p>
 */
public class MySQLConnection {

    /**
     * The single instance of the {@code MySQLConnection} class.
     */
    private static MySQLConnection instance;

    /**
     * Private constructor to prevent direct instantiation.
     * <p>
     * Loads the MySQL JDBC driver when the instance is created.
     * </p>
     */
    private MySQLConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // load MySQL driver once
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of {@code MySQLConnection}.
     * <p>
     * If the instance does not exist, it will be created.
     * This method is synchronized to ensure thread safety.
     * </p>
     *
     * @return the single {@code MySQLConnection} instance
     */
    public static synchronized MySQLConnection getInstance() {
        if (instance == null) {
            instance = new MySQLConnection();
        }
        return instance;
    }

    /**
     * Creates and returns a connection to the MySQL database.
     *
     * @return a {@link Connection} object representing the database connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        String dbUrl = System.getenv().getOrDefault(
                "BISTRO_DB_URL",
                "jdbc:mysql://localhost:3306/db_bistro?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Jerusalem"
        );
        String dbUser = System.getenv("BISTRO_DB_USER");
        String dbPassword = System.getenv("BISTRO_DB_PASSWORD");

        if (dbUser == null || dbUser.isBlank() || dbPassword == null) {
            throw new SQLException(
                    "Database credentials are not configured. "
                    + "Set BISTRO_DB_USER and BISTRO_DB_PASSWORD environment variables."
            );
        }

        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);

    }
}