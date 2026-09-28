package navigation;

/**
 * The {@code Navigation} class manages the navigation settings of the application,
 * including the current mode (App or Terminal) and the user's role (Manager or Agent).
 * <p>
 * It provides utility methods to get the correct FXML screens for role selection
 * and home page based on the current mode and role.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * Navigation.setMode(Navigation.Mode.APP);
 * Navigation.setRole(Navigation.Role.MANAGER);
 * String roleScreen = Navigation.getRoleSelectionScreen(); // "RoleSelectionApp.fxml"
 * String homeScreen = Navigation.getHomeByRole();          // "ManagerHomePage.fxml"
 * </pre>
 *
 * @author Mohamad
 * @version 1.0
 */
public class Navigation {

    // ===== MODE (App / Terminal) =====

    /**
     * Enum representing the possible application modes.
     */
    public enum Mode {
        /** Terminal mode (desktop/console interface) */
        TERMINAL,

        /** App mode (mobile or standalone GUI app) */
        APP
    }

    /** The current application mode */
    private static Mode currentMode;

    /**
     * Sets the current application mode.
     *
     * @param mode the mode to set (TERMINAL or APP)
     */
    public static void setMode(Mode mode) {
        currentMode = mode;
    }

    /**
     * Returns the FXML file name for the role selection screen
     * based on the current mode.
     *
     * @return "RoleSelectionTerminal.fxml" if in TERMINAL mode,
     *         "RoleSelectionApp.fxml" if in APP mode.
     */
    public static String getRoleSelectionScreen() {
        if (currentMode == Mode.TERMINAL) {
            return "RoleSelectionTerminal.fxml";
        } else {
            return "RoleSelectionApp.fxml";
        }
    }

    // ===== ROLE (Manager / Agent) =====

    /**
     * Enum representing the possible user roles.
     */
    public enum Role {
        /** Manager role */
        MANAGER,

        /** Agent / Representative role */
        AGENT
    }

    /** The current user role */
    private static Role currentRole;

    /**
     * Sets the current user role.
     *
     * @param role the role to set (MANAGER or AGENT)
     */
    public static void setRole(Role role) {
        currentRole = role;
    }

    /**
     * Returns the FXML file name for the home screen based on the current role.
     *
     * @return "ManagerHomePage.fxml" if the current role is MANAGER,
     *         "RepresentativeHomepage.fxml" if the current role is AGENT.
     */
    public static String getHomeByRole() {
        if (currentRole == Role.MANAGER) {
            return "ManagerHomePage.fxml";
        } else {
            return "RepresentativeHomepage.fxml";
        }
    }
}