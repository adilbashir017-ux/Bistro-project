package client;

import common.ChatIF;
import gui.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import logic.*;
import navigation.Navigation;
import ocsf.client.AbstractClient;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main OCSF client of the Bistro system.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Receives messages from the server and routes them to the relevant GUI controllers.</li>
 *   <li>Maintains a simple client-side "session" for the currently logged-in {@link Subscriber}.</li>
 *   <li>Opens additional windows (e.g., Payment, Subscriber Dashboard) when needed.</li>
 *   <li>Ensures UI updates are executed on the JavaFX Application Thread using {@link Platform#runLater(Runnable)}.</li>
 * </ul>
 */
public class ChatClient extends AbstractClient {

    /* ================= INSTANCE VARIABLES ================= */

    /**
     * Reference to the UI layer used by this client (console/GUI abstraction).
     */
    private final ChatIF clientUI;

    /**
     * Client-side session: the currently active logged-in subscriber for this running client instance.
     */
    private static Subscriber activeSubscriber = null;

    /**
     * The currently active payment controller (if a Payment window is open).
     */
    private static PaymentController activePaymentController;

    /**
     * The currently active reservation handler (terminal/app/guest screens) that should receive reservation callbacks.
     */
    private static ReservationHandler activeReservationHandler;

    /* ================= CALLBACKS ================= */

    /**
     * Optional callback used to deliver lists of orders to whoever registered it.
     * (May be {@code null} if not used.)
     */
    public static Consumer<List<Order>> ordersListCallback = null;

    /**
     * Optional callback used to deliver lists of visits to whoever registered it.
     * (May be {@code null} if not used.)
     */
    public static Consumer<List<VisitHistory>> visitsListCallback = null;

    /**
     * Creates a new chat client and initializes the underlying OCSF connection configuration.
     *
     * @param host     server host name or IP
     * @param port     server port
     * @param clientUI UI abstraction that represents the client interface
     * @throws IOException if the client cannot be initialized (OCSF constructor may throw)
     */
    public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);
        this.clientUI = clientUI;
    }

    /* ================= ACTIVE SUBSCRIBER (THIS CLIENT) ================= */

    /**
     * Updates the current active subscriber for this client session.
     *
     * @param sub the logged-in subscriber (or {@code null} to clear the session)
     */
    public static void setActiveSubscriber(Subscriber sub) {
        activeSubscriber = sub;
    }

    /**
     * @return the currently active (logged-in) subscriber, or {@code null} if not logged in
     */
    public static Subscriber getActiveSubscriber() {
        return activeSubscriber;
    }

    /* ================= ACTIVE CONTROLLERS ================= */

    /**
     * Sets the currently active payment controller that should receive payment result callbacks.
     *
     * @param ctrl active {@link PaymentController} (may be {@code null})
     */
    public static void setActivePaymentController(PaymentController ctrl) {
        activePaymentController = ctrl;
    }

    /**
     * Sets the currently active reservation handler that should receive reservation-related callbacks.
     *
     * @param handler active {@link ReservationHandler} (may be {@code null})
     */
    public static void setActiveReservationHandler(ReservationHandler handler) {
        activeReservationHandler = handler;
    }

    /**
     * @return the currently active {@link ReservationHandler}, or {@code null} if none is set
     */
    public static ReservationHandler getActiveReservationHandler() {
        return activeReservationHandler;
    }

    /* ================= SERVER → CLIENT ================= */

    /**
     * Handles every message arriving from the server.
     * <p>
     * This method inspects the incoming object type (typically {@link Response} or {@link Subscriber})
     * and updates the UI accordingly. All UI changes are executed via {@link Platform#runLater(Runnable)}.
     *
     * @param msg message sent from the server (may be {@code null})
     */
    @Override
    public void handleMessageFromServer(Object msg) {


        /* ================= RESPONSE ================= */
        if (msg instanceof Response r) {


            String status = r.getStatus();
            switch (status) {

            /* ---------- GUEST RESERVATION ---------- */
            case "FREE_TABLES_FOUND" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.handleFreeTables((int) r.getData());
            });

            case "FREE_TABLES_NOT_FOUND" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.orderFail("No tables found at this time.", (List<LocalTime>) r.getData());
            });

            case "ENTER_WAITING" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.handleFreeTables(0);
            });

            case "CODE_FOUND" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.codeSuccess((Order) r.getData());
            });

            case "CODE_NOT_FOUND" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.codeStatus("Reservation code not found.", "red");
            });

            case "RESERVATION_TOO_CLOSE" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError(r.getData().toString());
            });

            case "CHECK_IN" -> Platform.runLater(() -> {
                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.codeStatus("Welcome, You can sit at table " + r.getData() + "!", "green");
            });

            case "GET_HOURS" -> Platform.runLater(() -> {
                String openClose = (String) r.getData();

                SubscriberReservationAppController appCtrl = SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    appCtrl.updateTimes(openClose);
                }

                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.updateTimes(openClose);
                }

                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null) h.updateTimes(openClose);

            });

            /* ---------- MANAGER / AGENT FEATURES ---------- */
            case "CURRENT_CUSTOMERS_LIST" -> Platform.runLater(() -> {
                if (gui.CurrentCustomersController.activeController != null) {
                    gui.CurrentCustomersController.activeController.setCustomers((List<Order>) r.getData());
                }
            });

            case "ACTIVE_RESERVATIONS_LIST" -> Platform.runLater(() -> {
                if (gui.ActiveReservationsController.activeController != null) {
                    gui.ActiveReservationsController.activeController.setReservations((List<Order>) r.getData());
                }
            });

            case "WAITING_LIST" -> Platform.runLater(() -> {
                if (gui.ShowWaitingListController.activeController != null) {
                    gui.ShowWaitingListController.activeController.setWaitingList((List<Order>) r.getData());
                }
            });

            case "SUBSCRIBERS_LIST" -> Platform.runLater(() -> {
                if (SubscribersListController.activeController != null) {
                    SubscribersListController.activeController.setSubscribers((List<Subscriber>) r.getData());
                }
            });

            case "STAFF_LOGIN_SUCCESS" -> Platform.runLater(() -> {
                Staff staff = (Staff) r.getData();

                try {
                    if (staff.getRole().equals("MANAGER")) {
                        Navigation.setRole(Navigation.Role.MANAGER);
                        ManagerLoginController.getActive().showLoginSuccess();
                    } else {
                        Navigation.setRole(Navigation.Role.AGENT);
                        RepresentativeLoginController.getActive().showLoginSuccess();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            case "LOGIN_FAIL" -> Platform.runLater(() -> {
                if (gui.ManagerLoginController.getActive() != null) {
                    gui.ManagerLoginController.getActive().showLoginError();
                }
            });

            case "OPENING_HOURS_UPDATED" -> Platform.runLater(() -> {
                showInfo("Opening hours updated successfully ✅");
            });

            case "SPECIAL_DAY_SET" -> Platform.runLater(() -> {
                showInfo("Special day set successfully ✅");
            });

            case "SPECIAL_DAY_DELETED" -> Platform.runLater(() -> {
                showInfo("Special day deleted successfully ✅");
            });

            /* ---------- TABLES ---------- */
            case "TABLES_LIST" -> Platform.runLater(() -> {
                if (gui.TablesManagementController.activeController != null) {
                    gui.TablesManagementController.activeController
                            .setTables((List<Tables>) r.getData());
                }
            });

            case "TABLE_ADDED" -> Platform.runLater(() -> showInfo("Table added successfully ✅"));
            case "TABLE_DELETED" -> Platform.runLater(() -> showInfo("Table deleted successfully ✅"));
            case "TABLE_UPDATED" -> Platform.runLater(() -> showInfo("Table updated successfully ✅"));
            case "SEATED_TABLE" -> Platform.runLater(() -> showInfo("Sorry but this table is currently used"));

            case "TABLES_UPDATED" -> Platform.runLater(() -> {
                ClientUI.chat.sendToServer(new Request("GET_TABLES", null));
            });

            /* ---------- ALL RESERVATIONS ---------- */
            case "ALL_RESERVATIONS_LIST" -> Platform.runLater(() -> {
                if (gui.ViewReservationsController.activeController != null) {
                    gui.ViewReservationsController.activeController
                            .setReservations((List<Order>) r.getData());
                }
            });

            /* ---------- SUBSCRIBER CREATED ---------- */
            case "SUBSCRIBER_CREATED" -> Platform.runLater(() -> {
                int id = (int) r.getData();
                String displayId = String.format("%04d", id);

                if (gui.RegisterSubscriberController.activeController != null) {
                    gui.RegisterSubscriberController.activeController
                            .showSuccess(displayId);
                }
            });

            /* ---------- MANAGEMENT INFO ---------- */
            case "MANAGEMENT_INFO" -> Platform.runLater(() -> {
                Map<String, Integer> data = (Map<String, Integer>) r.getData();

                if (gui.ManagementInfoController.activeController != null) {
                    gui.ManagementInfoController.activeController.updateData(
                            data.get("today"),
                            data.get("month"),
                            data.get("canceled"),
                            data.get("subscribers"),
                            data.get("inside")
                    );
                }
            });

            /* ---------- REPORTS ---------- */
            case "REPORTS_DATA" -> Platform.runLater(() -> {
                if (gui.ReportsAnalysisController.activeController != null) {
                    gui.ReportsAnalysisController.activeController
                            .setReportsData((Map<String, Map<String, Integer>>) r.getData());
                }
            });

            case "CANCEL_SUCCESS" -> Platform.runLater(() -> {

                SubscriberReservationController subCtrl =
                        SubscriberReservationController.getActive();

                if (subCtrl != null) {
                    subCtrl.codeStatus("Order canceled.", "red");
                    return;
                }

                ReservationController resCtrl =
                        ReservationController.getInstance();

                if (resCtrl != null) {
                    resCtrl.codeStatus("Order canceled.", "red");
                }
            });

            case "CANCEL_FAILED" -> Platform.runLater(() -> {

                SubscriberReservationController subCtrl =
                        SubscriberReservationController.getActive();

                if (subCtrl != null) {
                    subCtrl.codeStatus("Cancel failed.", "red");
                    return;
                }

                ReservationController resCtrl =
                        ReservationController.getInstance();

                if (resCtrl != null) {
                    resCtrl.codeStatus("Cancel failed.", "red");
                }
            });

            /* ---------- PAYMENT ---------- */
            case "ORDER_PAYABLE" -> Platform.runLater(() -> {

                EntryPaymentCodeController epc = EntryPaymentCodeController.getActive();
                if (epc != null) epc.closeWindow();

                Map<?, ?> data = (Map<?, ?>) r.getData();
                int confirmationCode = (int) data.get("confirmationCode");
                double price = (double) data.get("price");

                Subscriber sub = ChatClient.getActiveSubscriber();

                openPaymentWindow(sub, confirmationCode, price);
            });

            case "ORDER_NOT_PAYABLE" -> Platform.runLater(() ->
                    showError("Payment not allowed.\nStatus: " + r.getData())
            );

            case "PAYMENT_SUCCESS" -> Platform.runLater(() -> {
                if (activePaymentController != null)
                    activePaymentController.showPaymentSuccess();
            });

            case "PAYMENT_FAILED" -> Platform.runLater(() -> {
                if (activePaymentController != null)
                    activePaymentController.showPaymentFailed();
            });

            /* ---------- SUBSCRIBER RESERVATION ---------- */
            case "AVAILABLE" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.sendCreateReservation();
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    appCtrl.sendCreateReservation();
                }
            });

            case "NOT_AVAILABLE" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.showSuggestedTimes((List<LocalTime>) r.getData());
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    appCtrl.showSuggestedTimes((List<LocalTime>) r.getData());
                }
            });

            case "RESERVATION_CREATED" -> Platform.runLater(() -> {
                int code = (int) ((Map<?, ?>) r.getData()).get("confirmationCode");
                showInfo("Reservation confirmed successfully and confirmation code sent to email\nConfirmation code: " + code);
            });

            case "LOST_CODE_SENT" -> Platform.runLater(() ->
                    showInfo("Your reservation codes were sent to your email.")
            );

            case "LOST_CODE_FAILED" -> Platform.runLater(() -> {
                showError(
                        "❌ No active reservations found.\n\n" +
                                "You currently don't have any upcoming bookings,\n" +
                                "or your reservations were already cancelled.\n\n" +
                                "Please make a new reservation if needed."
                );
            });

            /* ---------- CHECK-IN (Terminal only) ---------- */
            case "CHECKIN_SUCCESS" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    Map<?, ?> data = (Map<?, ?>) r.getData();
                    int tableNum = (int) data.get("table_num");
                    int code = (int) data.get("confirmation_code");
                    termCtrl.handleCheckInSuccess(tableNum, code);
                }
            });

            case "NO_TABLE_AVAILABLE" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInWait();

                ReservationHandler h = ChatClient.getActiveReservationHandler();
                if (h != null)
                    h.codeStatus("Please wait while we find you a free table", "white");
            });

            case "CHECKIN_CODE_NOT_FOUND" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError("Confirmation code not found.");
            });

            case "SUBSCRIBER_ACTIVE_ORDERS" -> Platform.runLater(() -> {
                SubscriberReservationController ctrl = SubscriberReservationController.getActive();
                if (ctrl != null) {
                    ctrl.populateOrders((List<String>) r.getData());
                }
            });

            case "CHECKIN_WRONG_DAY" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError("This reservation is not for today.");
            });

            case "CHECKIN_TOO_EARLY" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError("You arrived too early.");
            });

            case "CHECKIN_TOO_LATE" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError("You arrived too late.");
            });

            case "CHECKIN_NOT_ALLOWED" -> Platform.runLater(() -> {
                SubscriberReservationController termCtrl = SubscriberReservationController.getActive();
                if (termCtrl != null) termCtrl.handleCheckInError("Check-in not allowed.\n" + r.getData());
            });

            /* ---------- WAITING LIST ---------- */
            case "WAITING_SEATED" -> Platform.runLater(() -> {
                Map<?, ?> data = (Map<?, ?>) r.getData();
                showInfo(
                        "🎉 Table is ready!\n\n" +
                                "Table Number: " + data.get("table_num") + "\n" +
                                "Confirmation Code: " + data.get("confirmationCode")
                );
            });

            case "WAITING_JOINED" -> Platform.runLater(() -> {
                int code = (int) ((Map<?, ?>) r.getData()).get("confirmationCode");
                showInfo(
                        "✅ You have been added to the waiting list.\n\n" +
                                "Confirmation Code: " + code + "\n\n" +
                                "⏳ When a table becomes available, you will receive an email.\n" +
                                "Please wait patiently. Thank you!"
                );
            });

            case "WAITING_ALREADY_EXISTS" -> Platform.runLater(() -> {
                int code = (int) ((Map<?, ?>) r.getData()).get("confirmationCode");
                showInfo("ℹ Already in waiting list.\nCode: " + code);
            });

            case "WAITING_NOT_ALLOWED" ->
                    Platform.runLater(() -> showError((String) r.getData()));

            case "WAITING_LEFT" ->
                    Platform.runLater(() -> showInfo("✅ Left waiting list."));

            case "WAITING_NOT_FOUND" ->
                    Platform.runLater(() -> showError("Waiting entry not found."));

            case "WAITING_JOIN_FAILED" ->
                    Platform.runLater(() -> showError("Failed to join waiting list."));

            /* ---------- ACCOUNT HISTORY ---------- */
            case "ACCOUNT_VISITS" -> Platform.runLater(() -> {
                AccountHistoryController ctrl = AccountHistoryController.getActive();
                if (ctrl != null)
                    ctrl.handleVisitHistory((List<Order>) r.getData());
            });

            case "ACCOUNT_RESERVATIONS" -> Platform.runLater(() -> {
                AccountHistoryController ctrl = AccountHistoryController.getActive();
                if (ctrl != null)
                    ctrl.handleReservationHistory((List<Order>) r.getData());
            });

            /* ---------- SUBSCRIBER UPDATE DETAILS ---------- */
            case "UPDATE_EMAIL_SUCCESS" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.applyUpdatedEmail((String) r.getData());
                    termCtrl.codeStatus("Email updated successfully ✅", "green");
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    appCtrl.applyUpdatedEmail((String) r.getData());
                    showInfo("Email updated successfully ✅");
                }
            });

            case "UPDATE_EMAIL_FAILED" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.codeStatus("Failed to update email ❌", "red");
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    showError("Failed to update email ❌");
                }
            });

            case "UPDATE_PHONE_SUCCESS" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.applyUpdatedPhone((String) r.getData());
                    termCtrl.codeStatus("Phone updated successfully ✅", "green");
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    appCtrl.applyUpdatedPhone((String) r.getData());
                    showInfo("Phone updated successfully ✅");
                }
            });

            case "UPDATE_PHONE_FAILED" -> Platform.runLater(() -> {

                SubscriberReservationController termCtrl =
                        SubscriberReservationController.getActive();
                if (termCtrl != null) {
                    termCtrl.codeStatus("Failed to update phone ❌", "red");
                    return;
                }

                SubscriberReservationAppController appCtrl =
                        SubscriberReservationAppController.getActive();
                if (appCtrl != null) {
                    showError("Failed to update phone ❌");
                }
            });

            /* ---------- SUBSCRIBER LOGIN ---------- */
            case "SUBSCRIBER_LOGIN_SUCCESS" -> Platform.runLater(() -> {
                try {
                    Subscriber sub = (Subscriber) r.getData();

                    ChatClient.setActiveSubscriber(sub);

                    loadSubscriberMain(sub);
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Login succeeded but failed to open SubscriberMain.");
                }
            });

            case "ALREADY_LOGGED_IN" -> Platform.runLater(() -> {
                if (SubscriberLoginController.getActive() != null) {
                    SubscriberLoginController.getActive().showCustomError("Someone is currently using this account");
                }
            });

            case "SUBSCRIBER_LOGIN_FAIL" -> Platform.runLater(() -> {
                if (SubscriberLoginController.getActive() != null) {
                    SubscriberLoginController.getActive()
                            .showCustomError("Wrong Subscriber ID or Name");
                }
            });

            default -> System.out.println("[Client] Unhandled Response: " + status);
            }
            return;
        }

        /* ================= LOGIN (Fallback) ================= */
        if (msg instanceof Subscriber sub) {
            Platform.runLater(() -> {
                ChatClient.setActiveSubscriber(sub);

                loadSubscriberMain(sub);
            });
            return;
        }

        /* ================= ERROR ================= */
        if ("SERVER_ERROR".equals(msg)) {
            Platform.runLater(() ->
                    showError("Server error. Please try again later."));
        }
    }

    /* ================= HELPERS ================= */

    /**
     * Opens the payment window and passes the relevant payment data to the {@link PaymentController}.
     *
     * @param sub              subscriber performing the payment (may be {@code null} if not logged in)
     * @param confirmationCode reservation confirmation code to pay for
     * @param price            calculated total price to be charged
     */
    private void openPaymentWindow(Subscriber sub, int confirmationCode, double price) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Payment.fxml"));
            Parent root = loader.load();

            PaymentController ctrl = loader.getController();
            ctrl.setData(sub, confirmationCode, price);

            Stage stage = new Stage();
            stage.setTitle("Payment");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads and displays the subscriber dashboard screen (Terminal or App version) based on the last role selection screen.
     * Also closes the login window after successful navigation.
     *
     * @param sub the logged-in subscriber
     */
    private void loadSubscriberMain(Subscriber sub) {
        try {

            String fxml;
            if (navigation.Navigation.getRoleSelectionScreen().equals("RoleSelectionTerminal.fxml")) {
                fxml = "/gui/SubscriberMain.fxml";
            } else {
                fxml = "/gui/SubscriberMainApp.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (fxml.contains("App")) {
                SubscriberMainControllerApp ctrl = loader.getController();
                ctrl.setSubscriber(sub);
            } else {
                SubscriberMainController ctrl = loader.getController();
                ctrl.setSubscriber(sub);
            }

            Stage stage = new Stage();
            stage.setTitle("Subscriber Dashboard");
            stage.setScene(new Scene(root));
            stage.show();

            Platform.runLater(() -> {
                if (SubscriberLoginController.getActive() != null) {
                    Stage loginStage =
                            (Stage) SubscriberLoginController.getActive()
                                    .getTxtSubscriberId().getScene().getWindow();
                    loginStage.close();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a client UI message to the server without opening/closing the socket.
     * This is called by {@code ClientController.accept(...)} in the project.
     *
     * @param message message object to send to the server
     */
    public void handleMessageFromClientUI(Object message) {
        try {
            sendToServer(message);
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Failed to send message to server."));
        }
    }

    /**
     * Closes the client connection gracefully.
     * Any {@link IOException} during close is ignored.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException ignored) {
        }
    }

    /**
     * Displays an error alert dialog.
     *
     * @param msg error text to show
     */
    private static void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    /**
     * Displays an information alert dialog.
     *
     * @param msg information text to show
     */
    private static void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
