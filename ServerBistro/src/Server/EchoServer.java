package Server;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import logic.*;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * Main OCSF server for the Bistro system.
 * <p>
 * Responsible for receiving messages from connected clients, routing them to the
 * appropriate handler methods, and performing database operations using
 * {@link MySQLConnectionPool}. Also coordinates asynchronous email sending via
 * an internal {@link ExecutorService}.
 * </p>
 * <p>
 * The server supports subscriber/staff login, reservation lifecycle (create/cancel),
 * check-in flow (terminal + internal), payment flow, management queries, and waiting list features.
 * </p>
 */
public class EchoServer extends AbstractServer {

    /** Helper counter used by server logic when selecting tables. */
    int giveTable = 0;

    /** Holds an email address used by certain flows (e.g., confirmation emails). */
    String emailForCode = null;

    /** Tracks connected clients and their IP addresses. */
    private final Map<ConnectionToClient, String> clientIPs = new ConcurrentHashMap<>();

    /**
     * Tracks currently active subscribers by subscriber id → client connection.
     * Used to prevent concurrent usage / track online sessions.
     */
    private static final Map<Integer, ConnectionToClient> activeSubscribers = new ConcurrentHashMap<>();

    /**
     * Stores the time when a reservation (by confirmation code) was marked as SEATED.
     * Used for timing-related logic after check-in.
     */
    private final Map<Integer, LocalDateTime> seatedTimeByCode = new ConcurrentHashMap<>();

    /**
     * Single-thread executor used to send emails asynchronously so DB/network work
     * is not blocked by email sending.
     */
    private static final ExecutorService EMAIL_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("EMAIL_EXECUTOR");
        return t;
    });

    /**
     * Creates a new server instance bound to the given port.
     *
     * @param port server listen port
     */
    public EchoServer(int port) {
        super(port);
    }

    /**
     * Receives and handles any incoming message from a client.
     * <p>
     * A DB connection is acquired from {@link MySQLConnectionPool} for the duration of this call,
     * and released in a {@code finally} block. The message is then dispatched to a dedicated handler
     * method based on its runtime type and (for {@link Request}) its {@code status}.
     * </p>
     *
     * @param msg    incoming message object (e.g., {@link Request}, login requests, DTOs)
     * @param client client connection that sent the message
     */
    @Override
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {


        MySQLConnectionPool pool = null;
        Connection conn = null;

        try {
            pool = MySQLConnectionPool.getInstance();
            conn = pool.getConnection();

            if (msg instanceof SubscriberLoginRequest loginReq) {
                handleSubscriberLogin(conn, loginReq, client);
                return;
            }

            if (msg instanceof StaffLoginRequest staffReq) {
                handleStaffLogin(conn, staffReq, client);
                return;
            }
            if (msg instanceof Request) {
                Request req = (Request) msg;
                System.out.println("[CLIENT → SERVER] IP="
                    + client.getInetAddress().getHostAddress()
                    + ", Sent: " + req.getData());
            } else {
                System.out.println("[CLIENT → SERVER] IP="
                    + client.getInetAddress().getHostAddress()
                    + ", Sent: " + msg.getClass().getSimpleName());
            }


            if (msg instanceof Request req) {

                System.out.println("[SERVER -> CLIENT] IP=" + client.getInetAddress()+", Msg=" + msg.getClass().getSimpleName());

                switch (req.getStatus()) {

                case "CHECK_AVAILABILITY" -> handleCheckAvailability(conn, req, client);
                case "CREATE_RESERVATION" -> handleCreateReservation(conn, req, client);

                case "CHECK_ORDER_FOR_PAYMENT" -> handleCheckOrderForPayment(conn, req, client);
                case "PAY_ORDER", "PAY_BILL" -> handlePayOrder(conn, req, client);

                case "CANCEL_ORDER" -> handleCancelOrder(conn, req, client);

                case "TERMINAL_CHECKIN" -> handleTerminalCheckIn(conn, req, client);

                case "ACCOUNT_VISITS" -> sendVisitHistory(conn, (int) req.getData(), client);
                case "ACCOUNT_RESERVATIONS" -> sendReservationHistory(conn, (int) req.getData(), client);
                case "SET_OPEN_HOURS" -> handleSetOpeningHours(conn, req, client);
                case "SET_SPECIAL_DAY" -> handleSetSpecialDay(conn, req, client);
                case "DELETE_SPECIAL_DAY" -> handleDeleteSpecialDay(conn, req, client);
                case "GET_TABLES" -> handleGetTables(conn, client);
                case "ADD_TABLE" -> handleAddTable(conn, req, client);
                case "DELETE_TABLE" -> handleDeleteTable(conn, req, client);
                case "UPDATE_TABLE" -> handleUpdateTable(conn, req, client);
                case "GET_ALL_RESERVATIONS" -> handleGetAllReservations(conn, client);
                case "REGISTER_SUBSCRIBER" -> handleRegisterSubscriber(conn, req, client);
                case "GET_MANAGEMENT_INFO" -> handleGetManagementInfo(conn, client);
                case "GET_REPORTS_DATA" -> handleGetReportsData(conn, client);
                case "GET_ALL_SUBSCRIBERS" -> handleGetAllSubscribers(conn, client);
                case "GET_CURRENT_CUSTOMERS" -> handleGetCurrentCustomers(conn, client);
                case "GET_ACTIVE_RESERVATIONS" -> handleGetActiveReservations(conn, client);
                case "GET_WAITING_LIST" -> handleGetWaitingList(conn, req, client);
                case "LOGIN_WITH_QR" -> handleLoginWithQR(conn, (String) req.getData(), client);
                case "LOST_CONFIRMATION_CODE" -> handleLostConfirmationCode(conn, req, client);
                case "GET_ACTIVE_ORDERS" -> handleSubscriberActiveOrders(conn, req, client);

                case "JOIN_WAITING_LIST" -> handleJoinWaitingList(conn, req, client);
                case "LEAVE_WAITING_LIST" -> handleLeaveWaitingList(conn, req, client);

                case "FREE_TABLES" -> handleFreeTables(conn, req, client);
                case "FREE_TABLES_TERMINAL" -> handleFreeTablesTerminal(conn, req, client);
                case "INSERT_NEW_ORDER" -> handleInsertOrder(conn, req, client);
                case "CONFIRMATION_CODE" -> handleConfirmationCode(conn, req, client);
                case "SEND_CODE" -> handleSendCode(conn, req, client);
                case "CHECK_IN" -> handleCheckIn(conn, req, client);
                case "GET_HOURS" -> handleGetHours(conn, req, client);
                case "UPDATE_SUBSCRIBER_EMAIL" -> handleUpdateEmail(conn, req, client);
                case "UPDATE_SUBSCRIBER_PHONE" -> handleUpdatePhone(conn, req, client);
                case "SUBSCRIBER_LOGOUT" -> handleSubscriberLogout(client);

                default -> System.out.println("[SERVER] Unknown request: " + req.getStatus());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                client.sendToClient("SERVER_ERROR");
            } catch (Exception ignored) {
            }
        } finally {
            if (pool != null && conn != null)
                pool.releaseConnection(conn);
        }
    }

    /**
     * Sends all active reservation confirmation codes of a subscriber to their email.
     * <p>
     * Fetches upcoming reservations for the subscriber with {@code order_status='BOOKED'} and sends
     * them asynchronously using {@link EmailService}. If no active reservations exist, an error
     * response is returned.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the subscriber id ({@code int})
     * @param client requesting client connection
     * @throws Exception if a DB or client communication error occurs
     */
    private void handleLostConfirmationCode(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int subId = (int) req.getData();

        String sql = "SELECT order_date, order_time, confirmation_code, customer_email "
                + "FROM `order` WHERE subscriber_id=? AND order_status='BOOKED' " + "ORDER BY order_date, order_time";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subId);
            ResultSet rs = ps.executeQuery();

            String email = null;
            List<String> codes = new ArrayList<>();

            while (rs.next()) {
                String date = rs.getString("order_date");
                String time = rs.getString("order_time");
                String code = rs.getString("confirmation_code");
                email = rs.getString("customer_email");
                codes.add("• " + date + " at " + time + " — Code: " + code);
            }

            if (codes.isEmpty() || email == null) {
                client.sendToClient(new Response("LOST_CODE_FAILED", null));
                return;
            }

            String finalEmail = email;
            List<String> finalCodes = List.copyOf(codes);

            EMAIL_EXECUTOR.submit(() -> {
                try {
                    EmailService.LostCodeEmail(finalEmail, finalCodes);
                } catch (Exception ignored) {
                }
            });

            client.sendToClient(new Response("LOST_CODE_SENT", null));
        }
    }

    /**
     * Validates that a reservation is within allowed time limits.
     *
     * @param date requested reservation date
     * @param time requested reservation time
     * @return {@code null} if valid; otherwise a user-friendly error message
     */
    private String validateReservationWindow(LocalDate date, LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requested = LocalDateTime.of(date, time);

        if (requested.isBefore(now.plusHours(1)))
            return "Reservation must be at least 1 hour from now.";

        if (requested.isAfter(now.plusMonths(1)))
            return "Reservation cannot be more than 1 month from now.";

        return null;
    }

    /**
     * Parses a time string in either strict {@code HH:mm} or flexible {@code H:mm} format.
     *
     * @param s time string
     * @return parsed {@link LocalTime}
     */
    private LocalTime parseTimeFlexible(String s) {
        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
        }
    }

    /**
     * Maps guest count to the required table capacity bucket.
     *
     * @param g number of guests
     * @return required number of places (2, 4, 6, or 10)
     */
    private int requiredPlacesForGuests(int g) {
        if (g <= 2)
            return 2;
        if (g <= 4)
            return 4;
        if (g <= 6)
            return 6;
        return 10;
    }

    /**
     * Checks whether there is availability for a requested order time.
     * <p>
     * Validates the reservation time window, then counts tables of the required size
     * and overlapping reservations for that time. If no availability exists, suggested
     * alternative times are returned.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is an {@link Order}
     * @param client requesting client connection
     * @throws Exception if DB or client communication fails
     */

    private boolean hasReservationWithinTwoHours(
            Connection conn,
            int subscriberId,
            LocalDate date,
            LocalTime requestedTime) throws SQLException {

        LocalTime from = requestedTime.minusHours(2);
        LocalTime to   = requestedTime.plusHours(2);

        String sql = """
            SELECT COUNT(*)
            FROM `order`
            WHERE subscriber_id = ?
              AND order_date = ?
              AND order_time BETWEEN ? AND ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subscriberId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setTime(3, java.sql.Time.valueOf(from));
            ps.setTime(4, java.sql.Time.valueOf(to));

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private void handleCheckAvailability(
            Connection conn, Request req, ConnectionToClient client) throws Exception {

        Order o = (Order) req.getData();

        LocalDate d = LocalDate.parse(o.getOrder_date());
        LocalTime t = parseTimeFlexible(o.getOrder_time());

        // 🚫 2-hour window restriction
        if (hasReservationWithinTwoHours(conn, o.getSubscriber_id(), d, t)) {
            client.sendToClient(new Response(
                    "RESERVATION_TOO_CLOSE",
                    "You already have a reservation within 2 hours of this time"
            ));
            return;
        }

        String err = validateReservationWindow(d, t);
        if (err != null) {
            client.sendToClient(new Response("INVALID_RESERVATION_TIME", err));
            return;
        }

        int need = requiredPlacesForGuests(o.getNumber_of_guests());
        int total = countExactFitTables(conn, need);
        int used = countOverlapping(conn, d, t, need);

        if (used < total)
            client.sendToClient(new Response("AVAILABLE", null));
        else
            client.sendToClient(new Response(
                    "NOT_AVAILABLE",
                    suggestTimes(conn, d, t, need)
            ));
    }


    /**
     * Counts the number of tables with exactly the requested capacity.
     *
     * @param conn active DB connection
     * @param p    required places
     * @return number of tables with {@code places = p}
     * @throws SQLException if query fails
     */
    private int countExactFitTables(Connection conn, int p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tables WHERE places=?")) {
            ps.setInt(1, p);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Counts how many reservations overlap a given 2-hour window for a given table size bucket.
     *
     * @param conn active DB connection
     * @param d    requested date
     * @param t    requested start time
     * @param p    required places bucket (2/4/6/10)
     * @return number of overlapping orders
     * @throws SQLException if query fails
     */
    private int countOverlapping(Connection conn, LocalDate d, LocalTime t, int p) throws SQLException {

        String sql = "SELECT COUNT(*) FROM `order` " + "WHERE order_date=? "
                + "AND order_status IN ('BOOKED','SEATED','BILL_SENT','WAITING_SEATED','WAITING_CALLED') " + "AND (CASE "
                + "     WHEN number_of_guests<=2 THEN 2 " + "     WHEN number_of_guests<=4 THEN 4 "
                + "     WHEN number_of_guests<=6 THEN 6 " + "     ELSE 10 END) = ? "
                + "AND TIME(?) < ADDTIME(order_time,'02:00:00') " + "AND order_time < ADDTIME(TIME(?),'02:00:00')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(d));
            ps.setInt(2, p);
            ps.setTime(3, Time.valueOf(t));
            ps.setTime(4, Time.valueOf(t));

            ResultSet rs = ps.executeQuery();
            rs.next();

            return rs.getInt(1);
        }
    }

    /**
     * Logs out a subscriber by removing its connection from the active subscribers map.
     *
     * @param client the client connection to remove
     */
    private void handleSubscriberLogout(ConnectionToClient client) {
        activeSubscribers.entrySet().removeIf(entry -> {
            if (entry.getValue() == client) {
                System.out.println("[LOGOUT] Subscriber " + entry.getKey() + " logged out");
                return true;
            }
            return false;
        });
    }

    /**
     * Checks whether there exists any available 2-hour reservation window for today,
     * starting from a given time.
     *
     * @param conn   active DB connection
     * @param places required table places bucket
     * @param from   starting time to search from
     * @return {@code true} if at least one available 2-hour window exists; otherwise {@code false}
     * @throws SQLException if queries fail
     */
    private boolean existsAny2hWindowToday(Connection conn, int places, LocalTime from) throws SQLException {

        LocalTime OPEN = LocalTime.of(17, 0);
        LocalTime CLOSE = LocalTime.of(21, 30);

        LocalTime start = from;
        if (start.isBefore(OPEN))
            start = OPEN;

        LocalTime lastStart = CLOSE.minusHours(2);
        if (start.isAfter(lastStart))
            return false;

        for (LocalTime t = start; !t.isAfter(lastStart); t = t.plusMinutes(15)) {
            int total = countExactFitTables(conn, places);
            int used = countOverlapping(conn, LocalDate.now(), t, places);
            if (used < total)
                return true;
        }
        return false;
    }

    /**
     * Cancels an order by confirmation code and optionally sends a cancellation email.
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client requesting client connection
     * @throws Exception if DB or client communication fails
     */
    private void handleCancelOrder(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int code = (int) req.getData();
        String email = null;

        try (PreparedStatement get = conn.prepareStatement(
                "SELECT customer_email FROM `order` WHERE confirmation_code=?")) {
            get.setInt(1, code);
            ResultSet rs = get.executeQuery();
            if (rs.next())
                email = rs.getString(1);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE `order` SET order_status='CANCELLED_BY_USER', status_datetime=NOW(), table_num=NULL WHERE confirmation_code=?")) {

            ps.setInt(1, code);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                if (email != null) {
                    String finalEmail = email;
                    EMAIL_EXECUTOR.submit(() -> {
                        try {
                            EmailService.sendCancelEmail(finalEmail, code);
                        } catch (Exception ignored) {
                        }
                    });
                }
                client.sendToClient(new Response("CANCEL_SUCCESS", null));
            } else {
                client.sendToClient(new Response("CANCEL_FAILED", null));
            }
        }
    }

    /**
     * Performs check-in for one or multiple confirmation codes.
     * <p>
     * For each code: locks the order row, finds an available table of the required size,
     * updates the order to {@code SEATED}, assigns table, updates timestamps, commits the transaction,
     * and sends a {@code CHECKIN_SUCCESS} response.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request data is either {@code Integer} confirmation code or {@code List<Integer>}
     * @param client requesting client connection
     * @throws Exception if DB operations fail
     */
    private void handleCheckIn(Connection conn, Request req, ConnectionToClient client) throws Exception {
        Object data = req.getData();
        List<Integer> codes;

        if (data instanceof Integer) {
            codes = List.of((Integer) data);
        } else if (data instanceof List<?>) {
            codes = ((List<?>) data).stream()
                    .filter(x -> x instanceof Integer)
                    .map(x -> (Integer) x)
                    .toList();
        } else {
            client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", "Invalid data format."));
            return;
        }

        for (int code : codes) {
            conn.setAutoCommit(false);

            try {
                String orderSql = "SELECT * FROM `order` WHERE confirmation_code=? FOR UPDATE";
                Order o = null;

                try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                    ps.setInt(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        o = new Order(
                                rs.getInt("order_number"),
                                rs.getString("order_date"),
                                rs.getString("order_time"),
                                rs.getInt("number_of_guests"),
                                rs.getInt("confirmation_code"),
                                rs.getInt("subscriber_id"),
                                rs.getString("customer_name"),
                                rs.getString("customer_phone"),
                                rs.getString("customer_email"),
                                rs.getString("date_of_placing_order"),
                                rs.getInt("table_num"),
                                rs.getString("order_status"),
                                rs.getString("status_datetime"),
                                rs.getBoolean("reminder_sent"),
                                rs.getString("arrival_datetime")
                        );
                    }
                }

                if (o == null) {
                    conn.rollback();
                    client.sendToClient(new Response("CHECKIN_CODE_NOT_FOUND", code));
                    continue;
                }

                int places = requiredPlacesForGuests(o.getNumber_of_guests());

                String tableSql = """
                    SELECT t.table_num
                    FROM tables t
                    WHERE t.places = ?
                      AND t.table_num NOT IN (
                          SELECT o.table_num
                          FROM `order` o
                          WHERE o.table_num IS NOT NULL
                            AND o.order_status IN ('SEATED','BILL_SENT','WAITING')
                            AND o.order_date = ?
                            AND TIME(?) < ADDTIME(o.order_time,'02:00:00')
                            AND o.order_time < ADDTIME(TIME(?),'02:00:00')
                      )
                    ORDER BY t.table_num
                    LIMIT 1
                    FOR UPDATE
                """;

                Integer tableNum = null;
                try (PreparedStatement ps = conn.prepareStatement(tableSql)) {
                    ps.setInt(1, places);
                    ps.setDate(2, Date.valueOf(o.getOrder_date()));
                    ps.setTime(3, Time.valueOf(o.getOrder_time()));
                    ps.setTime(4, Time.valueOf(o.getOrder_time()));

                    ResultSet rs = ps.executeQuery();
                    if (rs.next())
                        tableNum = rs.getInt(1);
                }

                if (tableNum == null) {
                    try (PreparedStatement ups = conn.prepareStatement(
                            """
                            UPDATE `order`
                            SET order_status='WAITING',
                                status_datetime=?,
                                arrival_datetime=IFNULL(arrival_datetime, ?)
                            WHERE confirmation_code=?
                            """
                    )) {
                        ups.setString(1, LocalDateTime.now().toString());
                        ups.setString(2,LocalDateTime.now().toString());
                        ups.setInt(3, code);
                        int updateds =  ups.executeUpdate();
                        System.out.println(updateds);
                    }
                    conn.commit();
                    client.sendToClient(new Response("NO_TABLE_AVAILABLE", code));
                    continue;
                }


                String updateSql = """
                    UPDATE `order`
                    SET table_num=?,
                        order_status='SEATED',
                        status_datetime=NOW(),
                        arrival_datetime=NOW()
                    WHERE confirmation_code=?
                """;

                int updated;
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, tableNum);
                    ps.setInt(2, code);
                    updated = ps.executeUpdate();
                }
                seatedTimeByCode.put(code, LocalDateTime.now());

                conn.commit();
                client.sendToClient(new Response("CHECK_IN",tableNum));
                System.out.println("[CHECK-IN] Code " + code + " at table " + tableNum);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Terminal check-in flow for a single confirmation code.
     * <p>
     * Validates the reservation is for today and within allowed arrival window,
     * then assigns a table and updates the order status to {@code SEATED}.
     * Also supports the {@code WAITING_CALLED} → {@code SEATED} transition.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client requesting client connection
     * @throws Exception if DB or client communication fails
     */
    private void handleTerminalCheckIn(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int code = (int) req.getData();

        String orderSql = "SELECT order_date, order_time, number_of_guests, order_status, table_num, status_datetime "
                + "FROM `order` WHERE confirmation_code=?";

        try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                client.sendToClient(new Response("CHECKIN_CODE_NOT_FOUND", null));
                return;
            }

            LocalDate orderDate = rs.getDate("order_date").toLocalDate();
            LocalTime orderTime = rs.getTime("order_time").toLocalTime();
            int guests = rs.getInt("number_of_guests");
            String status = rs.getString("order_status");
            Integer tableNum = (Integer) rs.getObject("table_num");
            Timestamp stTs = rs.getTimestamp("status_datetime");

            if (!orderDate.equals(LocalDate.now())) {
                client.sendToClient(new Response("CHECKIN_WRONG_DAY", null));
                return;
            }

            if ("WAITING_CALLED".equals(status)) {

                if (tableNum == null) {
                    client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", "No table assigned."));
                    return;
                }

                if (stTs == null) {
                    client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", "Missing call time."));
                    return;
                }

                LocalDateTime calledAt = stTs.toLocalDateTime();
                if (LocalDateTime.now().isAfter(calledAt.plusMinutes(15))) {
                    client.sendToClient(new Response("CHECKIN_TOO_LATE", null));
                    return;
                }

                java.sql.Timestamp nowTs = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());

                int updated;
                try (PreparedStatement ups = conn.prepareStatement(
                        """
                        UPDATE `order`
                        SET order_status='SEATED',
                            order_time=CURTIME(),
                            status_datetime=?,
                            arrival_datetime=IFNULL(arrival_datetime, ?)
                        WHERE confirmation_code=? AND order_status='WAITING_CALLED'
                        """
                )) {
                    ups.setTimestamp(1, nowTs);
                    ups.setTimestamp(2, nowTs);
                    ups.setInt(3, code);
                    updated = ups.executeUpdate();
                }

                if (updated <= 0) {
                    client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", "Could not update order."));
                    return;
                }

                seatedTimeByCode.put(code, nowTs.toLocalDateTime());

                client.sendToClient(new Response("CHECKIN_SUCCESS",
                        Map.of("table_num", tableNum, "confirmation_code", code)));
                return;
            }

            if (!"BOOKED".equals(status)) {
                client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", status));
                return;
            }

            LocalTime now = LocalTime.now();
            if (now.isBefore(orderTime.minusMinutes(15))) {
                client.sendToClient(new Response("CHECKIN_TOO_EARLY", null));
                return;
            }
            if (now.isAfter(orderTime.plusMinutes(15))) {
                client.sendToClient(new Response("CHECKIN_TOO_LATE", null));
                return;
            }

            int neededPlaces = requiredPlacesForGuests(guests);

            String tableSql = "SELECT t.table_num "
                    + "FROM tables t "
                    + "WHERE t.places=? "
                    + "AND NOT EXISTS ( "
                    + "   SELECT 1 FROM `order` o "
                    + "   WHERE o.table_num = t.table_num "
                    + "   AND o.table_num IS NOT NULL "
                    + "   AND o.order_date = CURDATE() "
                    + "   AND o.order_status = 'SEATED' "
                    + "   AND TIME(NOW()) < ADDTIME(o.order_time,'02:00:00') "
                    + ") "
                    + "LIMIT 1";

            Integer newTable = null;
            try (PreparedStatement tps = conn.prepareStatement(tableSql)) {
                tps.setInt(1, neededPlaces);
                ResultSet trs = tps.executeQuery();
                if (trs.next())
                    newTable = trs.getInt(1);
            }
            if (newTable == null) {
                client.sendToClient(new Response("NO_TABLE_AVAILABLE", null));
                return;
            }

            Timestamp nowTs =Timestamp.valueOf(LocalDateTime.now());

            int updated;
            try (PreparedStatement ups = conn.prepareStatement(
                    """
                    UPDATE `order`
                    SET order_status='SEATED',
                        table_num=?,
                        status_datetime=?,
                        arrival_datetime=IFNULL(arrival_datetime, ?)
                    WHERE confirmation_code=? AND order_status='BOOKED'
                    """
            )) {
                ups.setInt(1, newTable);
                ups.setTimestamp(2, nowTs);
                ups.setTimestamp(3, nowTs);
                ups.setInt(4, code);
                updated = ups.executeUpdate();
            }

            if (updated <= 0) {
                client.sendToClient(new Response("CHECKIN_NOT_ALLOWED", "Could not update order."));
                return;
            }

            seatedTimeByCode.put(code, nowTs.toLocalDateTime());

            client.sendToClient(new Response("CHECKIN_SUCCESS",
                    Map.of("table_num", newTable, "confirmation_code", code)));
        }
    }
    /**
     * Updates a subscriber's email address.
     * <p>
     * Expects {@link Request#getData()} to be a {@link Map} containing:
     * <ul>
     *   <li>{@code subscriberId} (int)</li>
     *   <li>{@code email} (String)</li>
     * </ul>
     * If the email format is invalid (does not contain {@code @}), the method returns
     * {@code UPDATE_EMAIL_FAILED}. Otherwise, it updates {@code subscriber.subscriber_email}
     * and returns {@code UPDATE_EMAIL_SUCCESS} with the new email on success.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request containing subscriber id and new email
     * @param client client connection to respond to
     * @throws Exception if a database error or send-to-client error occurs
     */
    private void handleUpdateEmail(Connection conn, Request req, ConnectionToClient client) throws Exception {

        Map<?, ?> data = (Map<?, ?>) req.getData();

        int subscriberId = (int) data.get("subscriberId");
        String newEmail = (String) data.get("email");

        if (!newEmail.contains("@")) {
            client.sendToClient(new Response("UPDATE_EMAIL_FAILED", null));
            return;
        }

        try (PreparedStatement ps = conn
                .prepareStatement("UPDATE subscriber SET subscriber_email=? WHERE subscriber_id=?")) {

            ps.setString(1, newEmail);
            ps.setInt(2, subscriberId);

            if (ps.executeUpdate() > 0)
                client.sendToClient(new Response("UPDATE_EMAIL_SUCCESS", newEmail));
            else
                client.sendToClient(new Response("UPDATE_EMAIL_FAILED", null));
        }
    }

    /**
     * Updates a subscriber's phone number.
     * <p>
     * Expects {@link Request#getData()} to be a {@link Map} containing:
     * <ul>
     *   <li>{@code subscriberId} (int)</li>
     *   <li>{@code phone} (String)</li>
     * </ul>
     * Phone is validated with {@code \\d{9,10}}. On success returns {@code UPDATE_PHONE_SUCCESS}
     * with the new phone value; otherwise returns {@code UPDATE_PHONE_FAILED}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request containing subscriber id and new phone
     * @param client client connection to respond to
     * @throws Exception if a database error or send-to-client error occurs
     */
    private void handleUpdatePhone(Connection conn, Request req, ConnectionToClient client) throws Exception {

        Map<?, ?> data = (Map<?, ?>) req.getData();

        int subscriberId = (int) data.get("subscriberId");
        String newPhone = (String) data.get("phone");

        if (!newPhone.matches("\\d{9,10}")) {
            client.sendToClient(new Response("UPDATE_PHONE_FAILED", null));
            return;
        }

        try (PreparedStatement ps = conn
                .prepareStatement("UPDATE subscriber SET subscriber_phone=? WHERE subscriber_id=?")) {

            ps.setString(1, newPhone);
            ps.setInt(2, subscriberId);

            if (ps.executeUpdate() > 0)
                client.sendToClient(new Response("UPDATE_PHONE_SUCCESS", newPhone));
            else
                client.sendToClient(new Response("UPDATE_PHONE_FAILED", null));
        }
    }

    /**
     * Creates a new reservation for a subscriber.
     * <p>
     * Generates a unique 6-digit confirmation code, inserts a new row into {@code `order`}
     * with {@code order_status='BOOKED'}, and responds with {@code RESERVATION_CREATED}
     * including the confirmation code. Also triggers an asynchronous confirmation email
     * via {@link EmailService}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is an {@link Order}
     * @param client client connection to respond to
     * @throws Exception if insertion or client communication fails
     */
    private void handleCreateReservation(Connection conn, Request req, ConnectionToClient client) throws Exception {

        Order o = (Order) req.getData();
        int code = generateCode(conn);

        try (PreparedStatement ps = conn
                .prepareStatement("INSERT INTO `order` (order_date,order_time,number_of_guests,confirmation_code,"
                        + "subscriber_id,customer_name,customer_phone,customer_email,date_of_placing_order,order_status,status_datetime,reminder_sent) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,'BOOKED',NOW(),false)")) {

            ps.setDate(1, Date.valueOf(o.getOrder_date()));
            ps.setTime(2, Time.valueOf(o.getOrder_time() + ":00"));
            ps.setInt(3, o.getNumber_of_guests());
            ps.setInt(4, code);
            ps.setInt(5, o.getSubscriber_id());
            ps.setString(6, o.getCustomer_name());
            ps.setString(7, o.getCustomer_phone());
            ps.setString(8, o.getCustomer_email());
            ps.setString(9, LocalDateTime.now().toString());
            ps.executeUpdate();
        }

        client.sendToClient(new Response("RESERVATION_CREATED", Map.of("confirmationCode", code)));

        String email = o.getCustomer_email();
        EMAIL_EXECUTOR.submit(() -> {
            try {
                EmailService.sendConfirmationEmail(email, code);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    /**
     * Generates a unique 6-digit confirmation code that does not already exist in {@code `order`}.
     *
     * @param conn active DB connection
     * @return a unique confirmation code (100000–999999)
     * @throws SQLException if the existence check query fails
     */
    private int generateCode(Connection conn) throws SQLException {
        Random r = new Random();
        while (true) {
            int c = 100000 + r.nextInt(900000);
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM `order` WHERE confirmation_code=?")) {
                ps.setInt(1, c);
                if (!ps.executeQuery().next())
                    return c;
            }
        }
    }

    /**
     * Checks whether an order is eligible for payment and returns the payable amount.
     * <p>
     * Payment is allowed only when {@code order_status} is {@code SEATED} or {@code BILL_SENT}.
     * The price is calculated as {@code number_of_guests * 50.0}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleCheckOrderForPayment(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int code = (int) req.getData();

        String sql = "SELECT order_status, number_of_guests, subscriber_id " + "FROM `order` WHERE confirmation_code=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                client.sendToClient(new Response("ORDER_NOT_FOUND", null));
                return;
            }

            String status = rs.getString("order_status");
            int guests = rs.getInt("number_of_guests");

            if (!status.equals("SEATED") && !status.equals("BILL_SENT")) {
                client.sendToClient(new Response("ORDER_NOT_PAYABLE", status));
                return;
            }

            double price = guests * 50.0;

            client.sendToClient(new Response("ORDER_PAYABLE", Map.of("confirmationCode", code, "price", price)));
        }
    }

    /**
     * Processes payment for an order by confirmation code.
     * <p>
     * Steps:
     * <ol>
     *   <li>Fetch order details (order_number, email, subscriber_id, guests) and compute price.</li>
     *   <li>Update the order to {@code PAID}, set {@code status_datetime=NOW()}, and free the table.</li>
     *   <li>Send {@code PAYMENT_SUCCESS}/{@code PAYMENT_FAILED}.</li>
     *   <li>Insert a row into {@code payment} with the computed amount.</li>
     *   <li>Send a receipt email asynchronously (if email exists).</li>
     * </ol>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB operations fail
     */
    private void handlePayOrder(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int code = (int) req.getData();

        String email = null;
        Integer subscriberId = null;
        double price = 0;
        int guests = 0;
        int orderNum = 0;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT order_number,customer_email, subscriber_id, number_of_guests " +
                        "FROM `order` WHERE confirmation_code=?")) {

            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                orderNum = rs.getInt("order_number");
                email = rs.getString("customer_email");
                subscriberId = (Integer) rs.getObject("subscriber_id");
                guests = rs.getInt("number_of_guests");
                price = guests * 50;
            }
        }

        String sql = "UPDATE `order` SET order_status='PAID', status_datetime=NOW(), table_num=NULL " +
                "WHERE confirmation_code=? AND order_status IN ('SEATED','BILL_SENT','WAITING_SEATED')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, code);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                client.sendToClient(new Response("PAYMENT_SUCCESS", null));

                if (email != null) {
                    double price2 = guests * 50.0;

                    String finalEmail = email;
                    Integer finalSubscriberId = subscriberId;

                    EMAIL_EXECUTOR.submit(() -> {
                        try {
                            EmailService.sendPaymentReceiptEmail(finalEmail, code, price2, finalSubscriberId);
                        } catch (Exception ignored) {
                        }
                    });
                }

            } else {
                client.sendToClient(new Response("PAYMENT_FAILED", null));
            }
        }

        String paySql = "INSERT INTO payment (payment_id,order_number,amount) VALUES (0,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(paySql)) {
            ps.setInt(1, orderNum);
            ps.setDouble(2, price);
            int updated = ps.executeUpdate();
        }
    }

    /**
     * Adds a subscriber to the waiting list for today, or seats them immediately if a table is free.
     * <p>
     * Expects {@link Request#getData()} to be a {@link Map} containing:
     * <ul>
     *   <li>{@code subscriberId} (int)</li>
     *   <li>{@code guests} (int)</li>
     *   <li>{@code name} (String)</li>
     * </ul>
     * Behavior:
     * <ul>
     *   <li>Rejects invalid payload/fields or non-positive guests.</li>
     *   <li>Prevents duplicates for the same subscriber today with statuses
     *       {@code WAITING}, {@code WAITING_CALLED}, or {@code SEATED}.</li>
     *   <li>Rejects joining if there is no available 2-hour window later today for the needed table size.</li>
     *   <li>If a physical table is free now, inserts an order as {@code SEATED} and emails the subscriber.</li>
     *   <li>Otherwise inserts an order as {@code WAITING} and responds with {@code WAITING_JOINED}.</li>
     * </ul>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request containing waiting-list join data
     * @param client client connection to respond to
     * @throws Exception if DB operations or client communication fails
     */
    private void handleJoinWaitingList(Connection conn, Request req, ConnectionToClient client) throws Exception {

        Object obj = req.getData();
        if (!(obj instanceof Map<?, ?> data)) {
            client.sendToClient(new Response("WAITING_JOIN_FAILED", "Invalid data"));
            return;
        }

        int subscriberId;
        int guests;
        String name,arrival;

        try {
            subscriberId = (int) data.get("subscriberId");
            guests = (int) data.get("guests");
            name = (String) data.get("name");
            arrival = (String) data.get("arrival");
        } catch (Exception e) {
            client.sendToClient(new Response("WAITING_JOIN_FAILED", "Invalid fields"));
            return;
        }

        if (guests <= 0) {
            client.sendToClient(new Response("WAITING_JOIN_FAILED", "Invalid guests"));
            return;
        }

        try (PreparedStatement chk = conn.prepareStatement(
                "SELECT confirmation_code FROM `order` " +
                        "WHERE subscriber_id=? AND order_date=CURDATE() " +
                        "AND order_status IN ('WAITING','WAITING_CALLED','SEATED') " +
                        "ORDER BY status_datetime DESC LIMIT 1")) {

            chk.setInt(1, subscriberId);
            ResultSet crs = chk.executeQuery();

            if (crs.next()) {
                int existingCode = crs.getInt(1);
                client.sendToClient(new Response("WAITING_ALREADY_EXISTS",
                        Map.of("confirmationCode", existingCode)));
                return;
            }
        }

        String email, phone;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT subscriber_email, subscriber_phone FROM subscriber WHERE subscriber_id=?")) {

            ps.setInt(1, subscriberId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                client.sendToClient(new Response("WAITING_JOIN_FAILED", "Subscriber not found"));
                return;
            }

            email = rs.getString(1);
            phone = rs.getString(2);
        }

        LocalTime now = LocalTime.now();
        int needPlaces = requiredPlacesForGuests(guests);

        if (!existsAny2hWindowToday(conn, needPlaces, now)) {
            client.sendToClient(new Response("WAITING_NOT_ALLOWED",
                    "No table is available for any 2-hour window today. Please come another day."));
            return;
        }

        Integer tableNow = findFreePhysicalTableNow(conn, needPlaces);
        int code = generateCode(conn);

        if (tableNow != null) {

            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO `order` (order_date, order_time, arrivaldatetime, number_of_guests, confirmation_code, " +
                            "subscriber_id,customer_name, customer_phone, customer_email, date_of_placing_order, " +
                            "table_num, order_status, status_datetime,reminder_sent) " +
                            "VALUES (CURDATE(), CURTIME(), ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SEATED', NOW(),false)")) {

                ins.setString(1, arrival);
                ins.setInt(2, guests);
                ins.setInt(3, code);
                ins.setInt(4, subscriberId);
                ins.setString(5, name);
                ins.setString(6, phone);
                ins.setString(7, email);
                ins.setString(8, LocalDateTime.now().toString());
                ins.setInt(9, tableNow);
                ins.executeUpdate();
            }

            EMAIL_EXECUTOR.submit(() -> {
                try {
                    List<String> lines = List.of(
                            "✅ Your table is ready NOW!",
                            "Confirmation Code: " + code,
                            "Table Number: " + tableNow,
                            "Please come to the hostess/terminal now."
                    );
                    EmailService.LostCodeEmail(email, lines);
                } catch (Exception ignored) {
                }
            });

            client.sendToClient(
                    new Response("WAITING_SEATED",
                            Map.of("confirmationCode", code, "table_num", tableNow)));
            return;
        }

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO `order` (order_date, order_time, arrival_datetime, number_of_guests, confirmation_code, " +
                        "subscriber_id, customer_name, customer_phone, customer_email, date_of_placing_order, " +
                        "table_num, order_status, status_datetime,reminder_sent) " +
                        "VALUES (CURDATE(), CURTIME(), ?, ?, ?, ?, ?, ?, ?, NULL, 'WAITING', NOW(),false)")) {

            ins.setString(1, arrival);
            ins.setInt(2, guests);
            ins.setInt(3, code);
            ins.setInt(4, subscriberId);
            ins.setString(5, name);
            ins.setString(6, phone);
            ins.setString(7, email);
            ins.setString(8, LocalDateTime.now().toString());
            ins.executeUpdate();
        }

        client.sendToClient(new Response("WAITING_JOINED",
                Map.of("confirmationCode", code)));
    }

    /**
     * Removes a subscriber from the waiting list by cancelling the waiting order.
     * <p>
     * Expects {@link Request#getData()} to be the confirmation code ({@code int}).
     * Only orders with {@code order_status IN ('WAITING','WAITING_CALLED')} are affected.
     * On success, the status is updated to {@code CANCELLED_BY_USER} and an email is sent
     * asynchronously (if an email exists).
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB operations or client communication fails
     */
    private void handleLeaveWaitingList(Connection conn, Request req, ConnectionToClient client) throws Exception {

        int code = (int) req.getData();
        String email = null;

        try (PreparedStatement get = conn.prepareStatement(
                "SELECT customer_email FROM `order` WHERE confirmation_code=? AND order_status IN ('WAITING','WAITING_CALLED')")) {
            get.setInt(1, code);
            ResultSet rs = get.executeQuery();
            if (rs.next())
                email = rs.getString(1);
        }

        try (PreparedStatement ps = conn
                .prepareStatement("UPDATE `order` SET order_status='CANCELLED_BY_USER', status_datetime=NOW()"
                        + "WHERE confirmation_code=? AND order_status IN ('WAITING','WAITING_CALLED')")) {

            ps.setInt(1, code);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                if (email != null) {
                    String finalEmail = email;
                    EMAIL_EXECUTOR.submit(() -> {
                        try {
                            EmailService.sendWaitingCancelEmail(finalEmail, code);
                        } catch (Exception ignored) {
                        }
                    });
                }
                System.out.println("[WAITING LIST] Waiting list left");
                client.sendToClient(new Response("WAITING_LEFT", null));
            } else {
                client.sendToClient(new Response("WAITING_NOT_FOUND", null));
            }
        }
    }
    /**
     * Authenticates a subscriber by id and name and opens a session for this connection.
     * <p>
     * Uses a case-sensitive name match via {@code BINARY subscriber_name}.
     * Behavior:
     * <ul>
     *   <li>If no matching subscriber is found → sends {@code SUBSCRIBER_LOGIN_FAIL}.</li>
     *   <li>If the subscriber id is already present in {@code activeSubscribers} → sends {@code ALREADY_LOGGED_IN}.</li>
     *   <li>On success: creates a {@link Subscriber}, stores (subscriberId → client) in {@code activeSubscribers},
     *       and sends the {@link Subscriber} object to the client.</li>
     * </ul>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    login request containing subscriber id and name
     * @param client client connection to respond to
     * @throws Exception if parsing, DB, or client communication fails
     */
    private void handleSubscriberLogin(Connection conn, SubscriberLoginRequest req, ConnectionToClient client)
            throws Exception {

        try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM subscriber WHERE subscriber_id=? AND BINARY subscriber_name=?")) {

            int subscriberId = Integer.parseInt(req.getSubscriberId());

            ps.setInt(1, subscriberId);
            ps.setString(2, req.getSubscriberName());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                client.sendToClient(new Response("SUBSCRIBER_LOGIN_FAIL", null));
                return;
            }

            if (activeSubscribers.containsKey(subscriberId)) {
                client.sendToClient(new Response("ALREADY_LOGGED_IN", null));
                return;
            }

            Subscriber subscriber = new Subscriber(
                    rs.getInt("subscriber_id"),
                    rs.getString("subscriber_name"),
                    rs.getString("subscriber_email"),
                    rs.getString("subscriber_phone")
            );

            activeSubscribers.put(subscriberId, client);
            client.sendToClient(subscriber);
        }
    }

    /**
     * Sends visit history for a subscriber.
     * <p>
     * Retrieves orders for the given subscriber where status is one of:
     * {@code SEATED}, {@code BILL_SENT}, {@code PAID}, {@code COMPLETED},
     * ordered by date/time descending, and returns them in {@code ACCOUNT_VISITS}.
     * </p>
     *
     * @param conn         active DB connection
     * @param subscriberId subscriber id
     * @param client       client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void sendVisitHistory(Connection conn, int subscriberId, ConnectionToClient client) throws Exception {

        List<Order> list = new ArrayList<>();

        String sql = """
            SELECT * FROM `order`
            WHERE subscriber_id = ?
            AND order_status IN ('SEATED','BILL_SENT','PAID','COMPLETED')
            ORDER BY order_date DESC, order_time DESC
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subscriberId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        rs.getInt("confirmation_code"),
                        rs.getInt("subscriber_id"),
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"),
                        rs.getString("customer_email"),
                        rs.getString("date_of_placing_order"),
                        rs.getInt("table_num"),
                        rs.getString("order_status"),
                        rs.getString("status_datetime"),
                        rs.getBoolean("reminder_sent"),
                        rs.getString("arrival_datetime")
                ));
            }
        }

        System.out.println("VISITS HISTORY ROWS = " + list.size());

        client.sendToClient(new Response("ACCOUNT_VISITS", list));
    }

    /**
     * Sends reservation history for a subscriber.
     * <p>
     * Retrieves orders for the given subscriber where status is one of:
     * {@code BOOKED}, {@code SEATED}, {@code BILL_SENT}, {@code PAID}, {@code COMPLETED},
     * ordered by date/time descending, and returns them in {@code ACCOUNT_RESERVATIONS}.
     * </p>
     *
     * @param conn         active DB connection
     * @param subscriberId subscriber id
     * @param client       client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void sendReservationHistory(Connection conn, int subscriberId, ConnectionToClient client) throws Exception {

        List<Order> list = new ArrayList<>();

        String sql = """
            SELECT * FROM `order`
            WHERE subscriber_id = ?
            AND order_status IN ('BOOKED','SEATED','BILL_SENT','PAID','COMPLETED')
            ORDER BY order_date DESC, order_time DESC
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subscriberId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        rs.getInt("confirmation_code"),
                        rs.getInt("subscriber_id"),
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"),
                        rs.getString("customer_email"),
                        rs.getString("date_of_placing_order"),
                        rs.getInt("table_num"),
                        rs.getString("order_status"),
                        rs.getString("status_datetime"),
                        rs.getBoolean("reminder_sent"),
                        rs.getString("arrival_datetime")
                ));
            }
        }

        System.out.println("RESERVATIONS HISTORY ROWS = " + list.size());

        client.sendToClient(new Response("ACCOUNT_RESERVATIONS", list));
    }

    /**
     * Checks capacity for a requested reservation time and responds with availability.
     * <p>
     * Expects {@link Request#getData()} to be an {@link Order}. Determines the required
     * table size (places) from the guest count, then compares overlapping reservations
     * to total tables of that size.
     * <ul>
     *   <li>If capacity exists → sends {@code FREE_TABLES_FOUND} with {@code -1}.</li>
     *   <li>Otherwise → sends {@code FREE_TABLES_NOT_FOUND} with suggested times.</li>
     * </ul>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is an {@link Order}
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleFreeTables(Connection conn, Request req, ConnectionToClient client) throws Exception {
        Order o = (Order) req.getData();

        LocalDate date = LocalDate.parse(o.getOrder_date());
        LocalTime time = LocalTime.parse(o.getOrder_time());

        int places = requiredPlacesForGuests(o.getNumber_of_guests());

        int totalTables = countExactFitTables(conn, places);
        int overlapping = countOverlapping(conn, date, time, places);

        if (overlapping < totalTables) {
            int table = findFreePhysicalTable(conn, places, date, time);
            client.sendToClient(new Response("FREE_TABLES_FOUND", -1));
            System.out.println("[AVAILABLE] capacity ok for " + places + " places");
            return;
        }

        List<LocalTime> suggestions = suggestTimes(conn, date, time, places);

        client.sendToClient(new Response("FREE_TABLES_NOT_FOUND", suggestions));

        System.out.println("[NO CAPACITY] suggestions = " + suggestions);
    }

    /**
     * Terminal variant of free-tables check.
     * <p>
     * Expects {@link Request#getData()} to be an {@link Order}. If capacity exists,
     * tries to find a free physical table now and returns it via {@code FREE_TABLES_FOUND}.
     * If not available, returns {@code FREE_TABLES_FOUND} with {@code 0} indicating
     * the waiting-list path.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is an {@link Order}
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    public void handleFreeTablesTerminal(Connection conn, Request req, ConnectionToClient client) throws Exception {
        Order o = (Order) req.getData();

        LocalDate date = LocalDate.parse(o.getOrder_date());
        LocalTime time = LocalTime.parse(o.getOrder_time());

        int places = requiredPlacesForGuests(o.getNumber_of_guests());

        int totalTables = countExactFitTables(conn, places);
        int overlapping = countOverlapping(conn, date, time, places);

        if (overlapping < totalTables) {
            int table = findFreePhysicalTable(conn, places, date, time);
            client.sendToClient(new Response("FREE_TABLES_FOUND", table));
            System.out.println("[AVAILABLE] capacity ok for " + places + " places");
            return;
        }
        client.sendToClient(new Response("FREE_TABLES_FOUND", 0));
        System.out.println("[NO CAPACITY] Waiting List");
    }

    /**
     * Inserts a new order record into the database.
     * <p>
     * Expects {@link Request#getData()} to be an {@link Order}. Inserts into {@code `order`}
     * with {@code subscriber_id} as {@code NULL}. If {@code table_num==0} inserts {@code table_num=NULL},
     * otherwise uses the provided table number. On successful insert, sends a confirmation email.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is an {@link Order}
     * @param client client connection (not used for responses in this method)
     * @throws Exception if DB or email operations fail
     */
    private void handleInsertOrder(Connection conn, Request req, ConnectionToClient client) throws Exception {
        Order o = (Order) req.getData();
        if (o.getTable_num() == 0) {
            String sql = "INSERT INTO `order` (order_number,order_date,order_time,arrival_datetime,number_of_guests,confirmation_code,subscriber_id,customer_name,customer_phone,customer_email,date_of_placing_order,table_num,order_status,status_datetime,reminder_sent) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, NULL, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, o.getOrder_number());
                ps.setString(2, o.getOrder_date());
                ps.setString(3, o.getOrder_time());
                ps.setString(4, o.getArrival_datetime());
                ps.setInt(5, o.getNumber_of_guests());
                ps.setInt(6, o.getConfirmation_code());
                ps.setString(7, o.getCustomer_name());
                ps.setString(8, o.getCustomer_phone());
                ps.setString(9, o.getCustomer_email());
                ps.setString(10, o.getDate_of_placing_order());
                ps.setString(11, o.getOrder_status());
                ps.setString(12, o.getStatus_datetime());
                ps.setBoolean(13, o.getReminder_sent());

                int updated = ps.executeUpdate();

                if (updated > 0) {
                    EmailService.sendConfirmationEmail(o.getCustomer_email(), o.getConfirmation_code());
                    System.out.println("[DB] Order inserted: " + o);
                } else {
                    System.out.println("[DB] Order not inserted: " + o.getOrder_number());
                }
            }
        } else {
            String sql = "INSERT INTO `order` (order_number,order_date,order_time,arrival_datetime,number_of_guests,confirmation_code,subscriber_id,customer_name,customer_phone,customer_email,date_of_placing_order,table_num,order_status,status_datetime,reminder_sent) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, o.getOrder_number());
                ps.setString(2, o.getOrder_date());
                ps.setString(3, o.getOrder_time());
                ps.setString(4,o.getArrival_datetime());
                ps.setInt(5, o.getNumber_of_guests());
                ps.setInt(6, o.getConfirmation_code());
                ps.setString(7, o.getCustomer_name());
                ps.setString(8, o.getCustomer_phone());
                ps.setString(9, o.getCustomer_email());
                ps.setString(10, o.getDate_of_placing_order());
                ps.setInt(11, o.getTable_num());
                ps.setString(12, o.getOrder_status());
                ps.setString(13, o.getStatus_datetime());
                ps.setBoolean(14, o.getReminder_sent());

                int updated = ps.executeUpdate();

                if (updated > 0) {
                    EmailService.sendConfirmationEmail(o.getCustomer_email(), o.getConfirmation_code());
                    System.out.println("[DB] Order inserted: " + o);
                } else {
                    System.out.println("[DB] Order not inserted: " + o.getOrder_number());
                }
            }

        }

    }

    /**
     * Validates a confirmation code and returns the matching order if found.
     * <p>
     * Expects {@link Request#getData()} to be the confirmation code ({@code int}).
     * Searches for an order with status in:
     * {@code BOOKED}, {@code SEATED}, {@code WAITING}, {@code WAITING_CALLED},
     * {@code WAITING_SEATED}, {@code BILL_SENT}.
     * On success sends {@code CODE_FOUND} with the {@link Order}; otherwise sends {@code CODE_NOT_FOUND}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the confirmation code ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleConfirmationCode(Connection conn, Request req, ConnectionToClient client) throws Exception {
        int code = (int) req.getData();

        String orderSql = "SELECT * FROM `order` WHERE confirmation_code = ? AND order_status IN ('BOOKED', 'SEATED','WAITING','WAITING_CALLED','WAITING_SEATED','BILL_SENT')";

        try (PreparedStatement ops = conn.prepareStatement(orderSql)) {

            ops.setInt(1, code);

            try (ResultSet ors = ops.executeQuery()) {

                if (!ors.next()) {
                    client.sendToClient(new Response("CODE_NOT_FOUND", null));
                    System.out.println("[Confirmation Code] NOT FOUND");
                    return;
                }

                Order o = new Order(
                        ors.getInt("order_number"),
                        ors.getString("order_date"),
                        ors.getString("order_time"),
                        ors.getInt("number_of_guests"),
                        ors.getInt("confirmation_code"),
                        ors.getInt("subscriber_id"),
                        ors.getString("customer_name"),
                        ors.getString("customer_phone"),
                        ors.getString("customer_email"),
                        ors.getString("date_of_placing_order"),
                        ors.getInt("table_num"),
                        ors.getString("order_status"),
                        ors.getString("status_datetime"),
                        ors.getBoolean("reminder_sent"),
                        ors.getString("arrival_datetime")
                );

                Response response = new Response("CODE_FOUND", o);
                client.sendToClient(response);
                System.out.println("[Confirmation Code] FOUND");
            }
        }
    }

    /**
     * Sends all active confirmation codes for a given customer email.
     * <p>
     * Expects {@link Request#getData()} to be the email ({@link String}).
     * Collects codes for orders with status in:
     * {@code BOOKED}, {@code SEATED}, {@code WAITING}, {@code WAITING_CALLED},
     * {@code WAITING_SEATED}, {@code BILL_SENT}.
     * If no orders are found, the method only logs and returns without sending a response.
     * Otherwise it emails the list to the given address using {@link EmailService}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the email {@link String}
     * @param client client connection (not used for responses in this method)
     * @throws SQLException if DB operations fail
     */
    private void handleSendCode(Connection conn, Request req, ConnectionToClient client) throws SQLException {

        String email = (String) req.getData();

        String sql = "SELECT order_date, order_time, confirmation_code FROM `order` WHERE customer_email = ? AND order_status IN ('BOOKED', 'SEATED','WAITING','WAITING_CALLED','WAITING_SEATED','BILL_SENT') ORDER BY order_date, order_time";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            List<String> codes = new ArrayList<>();
            while (rs.next()) {
                String date = rs.getString("order_date");
                String time = rs.getString("order_time");
                String code = rs.getString("confirmation_code");

                codes.add("• " + date + " at " + time + " — Code: " + code);
            }

            if (codes.isEmpty()) {
                System.out.println("[LOST CODE] No orders found for email: " + email);
                return;
            }
            EmailService.LostCodeEmail(email, codes);
        }
    }

    /**
     * Returns opening hours for a given date, considering special days first.
     * <p>
     * Expects {@link Request#getData()} to be a {@link LocalDate}.
     * Workflow:
     * <ol>
     *   <li>Check {@code specialdays} for the date.</li>
     *   <li>If {@code is_all_day_close=true} → respond with {@code GET_HOURS} and {@code null}.</li>
     *   <li>If partial hours exist → respond with {@code GET_HOURS} and a string {@code "start end"}.</li>
     *   <li>If no special day → fallback to {@code openhours} for the weekday.</li>
     * </ol>
     * If there is no row in {@code openhours} for that day, the restaurant is considered closed.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is a {@link LocalDate}
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleGetHours(Connection conn, Request req, ConnectionToClient client) throws Exception {
        LocalDate date = (LocalDate) req.getData();

        String sqlSpecial = "SELECT is_all_day_close, start_time, end_time, reason FROM specialdays WHERE special_date = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSpecial)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                boolean allDayClose = rs.getBoolean("is_all_day_close");
                if (allDayClose) {
                    client.sendToClient(new Response("GET_HOURS", null));
                    return;
                } else {
                    LocalTime start = rs.getTime("start_time").toLocalTime();
                    LocalTime end = rs.getTime("end_time").toLocalTime();
                    client.sendToClient(new Response("GET_HOURS", start + " " + end));
                    return;
                }
            }
        }

        DayOfWeek day = date.getDayOfWeek();
        int dbDay = (day.getValue() % 7) + 1;

        String sqlOpen = "SELECT `open`, `close` FROM openhours WHERE `day` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlOpen)) {
            ps.setInt(1, dbDay);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LocalTime open = rs.getTime("open").toLocalTime();
                LocalTime close = rs.getTime("close").toLocalTime();
                System.out.println("openning hours for date " + date + ": " + open + " - " + close);
                client.sendToClient(new Response("GET_HOURS", open + " " + close));
            } else {
                client.sendToClient(new Response("GET_HOURS", null));
            }
        }
    }

    /**
     * Returns a list of the subscriber's active orders (booked or waiting).
     * <p>
     * Expects {@link Request#getData()} to be subscriber id ({@code int}).
     * Returns {@code SUBSCRIBER_ACTIVE_ORDERS} with a {@link List} of formatted strings.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is subscriber id ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleSubscriberActiveOrders(Connection conn, Request req, ConnectionToClient client) throws Exception {
        int subId = (int) req.getData();
        List<String> ordersList = new ArrayList<>();

        String sql = """
            SELECT order_date, order_time, confirmation_code, order_status
            FROM `order`
            WHERE subscriber_id = ? AND order_status IN ('BOOKED','WAITING')
            ORDER BY order_date, order_time
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String date = rs.getString("order_date");
                String time = rs.getString("order_time");
                String code = rs.getString("confirmation_code");
                String status = rs.getString("order_status");
                ordersList.add("• " + date + " at " + time + " — Code: " + code + " (" + status + ")");
            }
        }

        client.sendToClient(new Response("SUBSCRIBER_ACTIVE_ORDERS", ordersList));
    }

    /**
     * Suggests up to 3 alternative times for a reservation on the given date.
     * <p>
     * Logic:
     * <ol>
     *   <li>Reads open/close hours for the weekday from {@code openHours}.</li>
     *   <li>Tries times around the preferred time (±90/60/30, +30/60/90/120 minutes).</li>
     *   <li>If still missing suggestions, scans the entire open-close range in 30-minute steps.</li>
     * </ol>
     * A candidate time is included if:
     * <ul>
     *   <li>{@link #validateReservationWindow(LocalDate, LocalTime)} returns {@code null}</li>
     *   <li>and capacity exists according to {@link #countOverlapping(Connection, LocalDate, LocalTime, int)}
     *       and {@link #countExactFitTables(Connection, int)}.</li>
     * </ul>
     * </p>
     *
     * @param conn active DB connection
     * @param d    reservation date
     * @param t    preferred time
     * @param p    required table places
     * @return list of suggested times (up to 3)
     * @throws SQLException if DB operations fail
     */
    private List<LocalTime> suggestTimes(Connection conn, LocalDate d, LocalTime t, int p) throws SQLException {
        List<LocalTime> out = new ArrayList<>();

        int dayNumber = d.getDayOfWeek().getValue();

        String sql = "SELECT open, close FROM openHours WHERE day = ?";
        LocalTime open, close;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dayNumber);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return out;
            }

            open = LocalTime.parse(rs.getString("open"));
            close = LocalTime.parse(rs.getString("close"));
        }

        int[] around = { -90, -60, -30, 30, 60, 90, 120 };

        for (int mins : around) {
            LocalTime c = t.plusMinutes(mins);

            if (c.isBefore(open) || c.isAfter(close))
                continue;

            if (validateReservationWindow(d, c) == null
                    && countOverlapping(conn, d, c, p) < countExactFitTables(conn, p)) {

                out.add(c);
                if (out.size() == 3)
                    return out;
            }
        }

        for (LocalTime c = open; !c.isAfter(close); c = c.plusMinutes(30)) {

            if (validateReservationWindow(d, c) != null)
                continue;

            if (countOverlapping(conn, d, c, p) < countExactFitTables(conn, p)) {
                if (!out.contains(c))
                    out.add(c);
                if (out.size() == 3)
                    break;
            }
        }

        return out;
    }

    /**
     * Called automatically when the server starts listening.
     * <p>
     * Prints the listening port and schedules periodic trigger processing via {@link #runTriggers()}.
     * The scheduled task runs immediately and then every minute.
     * </p>
     */
    @Override
    protected void serverStarted() {
        System.out.println("[Server] Listening on port " + getPort());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                runTriggers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    /**
     * Periodic server-side trigger routine.
     * <p>
     * Opens a new DB connection from {@link MySQLConnectionPool} and performs:
     * <ul>
     *   <li>Reminder emails 2 hours before booked reservations (once per reservation) and sets {@code reminder_sent=true}.</li>
     *   <li>Auto-cancel {@code BOOKED} reservations as {@code CANCELLED_NO_SHOW} after 15 minutes.</li>
     *   <li>Auto-cancel {@code WAITING_CALLED} entries as {@code CANCELLED_NO_SHOW} after 15 minutes.</li>
     *   <li>After 2 hours seated: sends bill/receipt email and updates status to {@code BILL_SENT}.</li>
     *   <li>Calls the next waiting subscriber if possible via {@code callNextWaitingIfPossible(conn)}.</li>
     * </ul>
     * </p>
     *
     * @throws SQLException if DB operations fail
     */
    private void runTriggers() throws SQLException {

        try (Connection conn = MySQLConnectionPool.getInstance().getConnection()) {

            String reminderSql =
                    "SELECT confirmation_code, customer_email " +
                            "FROM `order` " +
                            "WHERE order_status='BOOKED' " +
                            "AND reminder_sent = FALSE " +
                            "AND TIMESTAMP(order_date, order_time) <= NOW() + INTERVAL 2 HOUR " +
                            "AND TIMESTAMP(order_date, order_time) > NOW()";

            try (PreparedStatement ps = conn.prepareStatement(reminderSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int code = rs.getInt("confirmation_code");
                    String email = rs.getString("customer_email");

                    EMAIL_EXECUTOR.submit(() -> {
                        try {
                            EmailService.sendReminderEmail(email);
                        } catch (Exception ignored) {
                        }
                    });

                    try (PreparedStatement ups = conn.prepareStatement(
                            "UPDATE `order` SET reminder_sent=TRUE WHERE confirmation_code=?")) {
                        ups.setInt(1, code);
                        ups.executeUpdate();
                    }
                }
            }

            conn.prepareStatement(
                    "UPDATE `order` " +
                            "SET order_status='CANCELLED_NO_SHOW', status_datetime=NOW(), table_num=NULL " +
                            "WHERE order_status='BOOKED' " +
                            "AND TIMESTAMP(order_date, order_time) <= NOW() - INTERVAL 15 MINUTE"
            ).executeUpdate();

            conn.prepareStatement(
                    "UPDATE `order` " +
                            "SET order_status='CANCELLED_NO_SHOW', status_datetime=NOW(), table_num=NULL " +
                            "WHERE order_status='WAITING_CALLED' " +
                            "AND status_datetime <= NOW() - INTERVAL 15 MINUTE"
            ).executeUpdate();

            String billSql =
                    "SELECT order_number, confirmation_code, customer_email, number_of_guests, subscriber_id " +
                            "FROM `order` " +
                            "WHERE order_status='SEATED' " +
                            "AND status_datetime <= NOW() - INTERVAL 2 HOUR";

            try (PreparedStatement ps = conn.prepareStatement(billSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int orderNum = rs.getInt("order_number");
                    int code = rs.getInt("confirmation_code");
                    String email = rs.getString("customer_email");
                    int guests = rs.getInt("number_of_guests");
                    Integer subId = (Integer) rs.getObject("subscriber_id");

                    double price = guests * 50.0;
                    int isSubscriber = (subId != null) ? 1 : 0;

                    EMAIL_EXECUTOR.submit(() -> {
                        try {
                            EmailService.sendPaymentReceiptEmail(
                                    email,
                                    code,
                                    price,
                                    isSubscriber
                            );
                        } catch (Exception ignored) {
                        }
                    });

                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE `order` SET order_status='BILL_SENT' WHERE order_number=?")) {
                        update.setInt(1, orderNum);
                        update.executeUpdate();
                    }
                }
            }

            callNextWaitingIfPossible(conn);
        }
    }
    /**
     * Checks the waiting queue (oldest first) and, if a suitable table is free right now,
     * calls the next subscriber by moving their order from {@code WAITING} to {@code WAITING_CALLED}.
     * <p>
     * Flow:
     * <ul>
     *   <li>Selects today's {@code WAITING} orders ordered by {@code status_datetime} ascending.</li>
     *   <li>For each order, calculates required places and tries to find a free physical table now.</li>
     *   <li>If no table is available right now, stops immediately (keeps FIFO fairness).</li>
     *   <li>If a table is available, updates the order to {@code WAITING_CALLED}, assigns {@code table_num},
     *       updates {@code status_datetime=NOW()}, and sends a notification email (if email exists).</li>
     *   <li>Stops after calling exactly one order.</li>
     * </ul>
     * </p>
     *
     * @param conn active DB connection
     * @throws SQLException if DB operations fail
     */
    private void callNextWaitingIfPossible(Connection conn) throws SQLException {

        String waitingSql = "SELECT confirmation_code, number_of_guests, customer_email " + "FROM `order` "
                + "WHERE order_status='WAITING' AND order_date=CURDATE() " + "ORDER BY status_datetime ASC";

        try (PreparedStatement ps = conn.prepareStatement(waitingSql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                int code = rs.getInt(1);
                int guests = rs.getInt(2);
                String email = rs.getString(3);

                int places = requiredPlacesForGuests(guests);

                Integer table = findFreePhysicalTableNow(conn, places);
                if (table == null)
                    return; // no table right now

                try (PreparedStatement ups = conn.prepareStatement(
                        "UPDATE `order` SET order_status='WAITING_CALLED', table_num=?, status_datetime=NOW() "
                                + "WHERE confirmation_code=? AND order_status='WAITING'")) {
                    ups.setInt(1, table);
                    ups.setInt(2, code);
                    int updated = ups.executeUpdate();

                    if (updated > 0) {
                        if (email != null && !email.isBlank()) {
                            int finalTable = table;
                            EMAIL_EXECUTOR.submit(() -> {
                                try {
                                    List<String> lines = List.of(
                                            "📣 Your table is ready!",
                                            "Confirmation Code: " + code,
                                            "Table Number: " + finalTable,
                                            "⚠ Please arrive within 15 minutes (Terminal Check-In)."
                                    );
                                    EmailService.LostCodeEmail(email, lines);
                                } catch (Exception ignored) {
                                }
                            });
                        }

                        return;
                    }
                }
            }
        }
    }

    /**
     * Retrieves all restaurant tables from the {@code tables} table and sends them to the client.
     * <p>
     * Responds with {@code TABLES_LIST} and a {@link List} of {@link Tables} objects.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleGetTables(Connection conn, ConnectionToClient client) throws SQLException {

        List<Tables> list = new ArrayList<>();

        String sql = "SELECT table_num, places FROM tables";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Tables(
                        rs.getInt("table_num"),
                        rs.getInt("places")
                ));
            }
        }

        try {
            client.sendToClient(new Response("TABLES_LIST", list));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new table with the given capacity to the {@code tables} table.
     * <p>
     * Expects {@link Request#getData()} to be the capacity ({@code int}).
     * Chooses {@code table_num} as {@code MAX(table_num)+1} (or {@code 1} if table is empty),
     * inserts the new row, and then sends the updated list via {@link #handleGetTables(Connection, ConnectionToClient)}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is capacity ({@code int})
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleAddTable(Connection conn,
            Request req,
            ConnectionToClient client) throws SQLException {

        int capacity = (int) req.getData();

        int nextTableNum = 1;
        String maxSql = "SELECT MAX(table_num) FROM tables";

        try (PreparedStatement ps = conn.prepareStatement(maxSql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getInt(1) > 0) {
                nextTableNum = rs.getInt(1) + 1;
            }
        }

        String insertSql = "INSERT INTO tables (table_num, places) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, nextTableNum);
            ps.setInt(2, capacity);
            ps.executeUpdate();
        }

        handleGetTables(conn, client);
    }

    /**
     * Deletes a table if it is not currently used by active orders, and revalidates future capacity.
     * <p>
     * Expects {@link Request#getData()} to be {@code tableNum} ({@code int}).
     * Steps:
     * <ol>
     *   <li>If there are active orders on that table (SEATED/BILL_SENT/WAITING_CALLED/WAITING_SEATED) → respond {@code SEATED_TABLE}.</li>
     *   <li>Fetch the table capacity (places).</li>
     *   <li>Find booked orders (status {@code BOOKED}) that could rely on this capacity.</li>
     *   <li>If removing the table breaks capacity at some booking time, cancel affected orders as {@code CANCELLED_BY_STAFF}
     *       and notify customers by email.</li>
     *   <li>Delete the table and then send refreshed table list.</li>
     * </ol>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is table number ({@code int})
     * @param client client connection to respond to
     * @throws Exception if DB, email, or client communication fails
     */
    private void handleDeleteTable(Connection conn, Request req, ConnectionToClient client) throws Exception {
        int tableNum = (int) req.getData();

        // 1️⃣ Check if the table is currently in use (seated orders)
        String seatedOrdersSql = "SELECT order_number FROM `order` " +
                "WHERE table_num = ? AND order_status IN ('BILL_SENT','WAITING_CALLED','SEATED','WAITING_SEATED')";
        try (PreparedStatement ps = conn.prepareStatement(seatedOrdersSql)) {
            ps.setInt(1, tableNum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("[TABLE DELETE] Table " + tableNum + " is currently occupied.");
                client.sendToClient(new Response("SEATED_TABLE", null));
                return;
            }
        }

        // 2️⃣ Get the capacity of the table being deleted
        int deletedTableCapacity = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT places FROM tables WHERE table_num = ?")) {
            ps.setInt(1, tableNum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                deletedTableCapacity = rs.getInt("places");
            } else {
                System.out.println("[TABLE DELETE] Table " + tableNum + " does not exist.");
                return;
            }
        }

        // 3️⃣ Get all booked orders that could potentially use this table
        String bookedOrdersSql = "SELECT * FROM `order` WHERE order_status='BOOKED' AND order_date >= CURDATE()";
        List<Order> bookedOrders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(bookedOrdersSql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bookedOrders.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        rs.getInt("confirmation_code"),
                        rs.getInt("subscriber_id"),
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"),
                        rs.getString("customer_email"),
                        rs.getString("date_of_placing_order"),
                        rs.getInt("table_num"),
                        rs.getString("order_status"),
                        rs.getString("status_datetime"),
                        rs.getBoolean("reminder_sent"),
                        rs.getString("arrival_datetime")
                ));
            }
        }

        // 4️⃣ Check each booked order if it can still be seated after table deletion
        for (Order o : bookedOrders) {
            LocalDate orderDate = LocalDate.parse(o.getOrder_date());
            LocalTime orderTime = LocalTime.parse(o.getOrder_time());

            int neededSeats = requiredPlacesForGuests(o.getNumber_of_guests());

            // Count all available tables that can accommodate this order, excluding the deleted table
            int availableTables = countAvailableTables(conn, neededSeats, tableNum, orderDate, orderTime);

            // Cancel only if no suitable table remains
            if (availableTables == 0) {
                try (PreparedStatement psCancel = conn.prepareStatement(
                        "UPDATE `order` SET order_status='CANCELLED_BY_STAFF', status_datetime=NOW() WHERE order_number=?")) {
                    psCancel.setInt(1, o.getOrder_number());
                    psCancel.executeUpdate();
                    System.out.println("[TABLE DELETE] Order " + o.getOrder_number() + " canceled due to table deletion.");
                    EmailService.StaffCancelTableEmail(o.getCustomer_email());
                }
            }
        }

        // 5️⃣ Delete the table
        try (PreparedStatement psDelete = conn.prepareStatement("DELETE FROM tables WHERE table_num=?")) {
            psDelete.setInt(1, tableNum);
            psDelete.executeUpdate();
            System.out.println("[TABLE DELETE] Table " + tableNum + " deleted successfully.");
        }

        // 6️⃣ Send updated table list to client
        handleGetTables(conn, client);
    }

    /**
     * Counts the number of tables that can seat a given number of guests and are free at the given date/time,
     * excluding a specific table (like the one being deleted).
     */
    private int countAvailableTables(Connection conn, int neededSeats, int excludeTableNum, LocalDate date, LocalTime time) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM tables t " +
                "WHERE t.places >= ? AND t.table_num <> ? " +
                "AND t.table_num NOT IN (" +
                "   SELECT o.table_num FROM `order` o " +
                "   WHERE o.order_date = ? AND o.order_time = ? " +
                "   AND o.order_status IN ('SEATED','WAITING_SEATED','WAITING_CALLED','BILL_SENT')" +
                ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, neededSeats);
            ps.setInt(2, excludeTableNum);
            ps.setString(3, date.toString());
            ps.setString(4, time.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("cnt");
        }
        return 0;
    }


    /**
     * Updates a table's capacity and revalidates future bookings for capacity violations.
     * <p>
     * Expects {@link Request#getData()} to be a {@link Tables} object.
     * Behavior:
     * <ul>
     *   <li>Blocks update if the table is currently used by active orders
     *       (SEATED/BILL_SENT/WAITING_CALLED/WAITING_SEATED) and responds {@code SEATED_TABLE}.</li>
     *   <li>Loads old capacity from DB.</li>
     *   <li>Iterates future {@code BOOKED} orders and recalculates capacity impact when replacing oldPlaces with newPlaces.</li>
     *   <li>If capacity breaks for any booking time, cancels affected order as {@code CANCELLED_BY_STAFF}
     *       (with {@code status_datetime=NOW()}) and notifies customer by email.</li>
     * </ul>
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is {@link Tables}
     * @param client client connection to respond to
     * @throws Exception if DB, email, or client communication fails
     */
    // Update table
        private void handleUpdateTable(Connection conn, Request req, ConnectionToClient client) throws SQLException {

            Tables t = (Tables) req.getData();

            String sql = "UPDATE tables SET places=? WHERE table_num=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, t.getPlaces());
                ps.setInt(2, t.getTable_num());
                ps.executeUpdate();
            }

            try {
                client.sendToClient(new Response("TABLE_UPDATED", null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    /**
     * Retrieves all reservations and sends them to the client.
     * <p>
     * Returns {@code ALL_RESERVATIONS_LIST} with a list of {@link Order} objects
     * populated from selected columns. Fields not selected are sent as {@code 0}/{@code null}.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleGetAllReservations(Connection conn,
            ConnectionToClient client) throws SQLException {

        List<Order> list = new ArrayList<>();
        String sql = """
                    SELECT order_number, order_date, order_time,
                           number_of_guests, order_status,
                           customer_name, customer_phone, customer_email
                    FROM `order`
                    ORDER BY order_date DESC, order_time DESC
                    """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        0,
                        0,
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"),
                        rs.getString("customer_email"),
                        null,
                        0,
                        rs.getString("order_status"),
                        null,
                        false,
                        null
                ));
            }

        }

        try {
            client.sendToClient(
                    new Response("ALL_RESERVATIONS_LIST", list)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Registers a new subscriber in the {@code subscriber} table and returns the generated id.
     * <p>
     * Expects {@link Request#getData()} to be a {@link Subscriber}.
     * Inserts {@code subscriber_name}, {@code subscriber_email}, {@code subscriber_phone},
     * then sends {@code SUBSCRIBER_CREATED} with the generated key.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is a {@link Subscriber}
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleRegisterSubscriber(
            Connection conn,
            Request req,
            ConnectionToClient client) throws SQLException {

        Subscriber sub = (Subscriber) req.getData();

        String sql =
                "INSERT INTO subscriber (subscriber_name, subscriber_email, subscriber_phone) " +
                        "VALUES (?, ?, ?)";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sub.getUsername());
            ps.setString(2, sub.getEmail());
            ps.setString(3, sub.getPhone());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int id = 0;
            if (rs.next()) {
                id = rs.getInt(1);
            }

            client.sendToClient(
                    new Response("SUBSCRIBER_CREATED", id)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Collects management counters for dashboard KPIs and sends them to the client.
     * <p>
     * Produces counts for:
     * <ul>
     *   <li>Today's reservations</li>
     *   <li>This month's reservations</li>
     *   <li>All cancelled reservations (status like {@code CANCELLED%})</li>
     *   <li>Total subscribers</li>
     *   <li>Current customers inside (status {@code SEATED})</li>
     * </ul>
     * Responds with {@code MANAGEMENT_INFO} and a map: {@code today, month, canceled, subscribers, inside}.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     * @throws IOException  if sending response fails
     */
    private void handleGetManagementInfo(
            Connection conn,
            ConnectionToClient client) throws SQLException, IOException {

        int todayReservations = 0;
        int monthlyReservations = 0;
        int canceledReservations = 0;
        int subscribers = 0;
        int currentCustomers = 0;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM `order` WHERE order_date = CURDATE()")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) todayReservations = rs.getInt(1);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM `order` WHERE MONTH(order_date)=MONTH(CURDATE()) AND YEAR(order_date)=YEAR(CURDATE())")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) monthlyReservations = rs.getInt(1);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM `order` WHERE order_status LIKE 'CANCELLED%'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) canceledReservations = rs.getInt(1);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM subscriber")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) subscribers = rs.getInt(1);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM `order` WHERE order_status='SEATED'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentCustomers = rs.getInt(1);
        }

        Map<String, Integer> data = new HashMap<>();
        data.put("today", todayReservations);
        data.put("month", monthlyReservations);
        data.put("canceled", canceledReservations);
        data.put("subscribers", subscribers);
        data.put("inside", currentCustomers);

        client.sendToClient(new Response("MANAGEMENT_INFO", data));
    }

    /**
     * Builds report datasets for the management reports screen and sends them to the client.
     * <p>
     * Produces:
     * <ul>
     *   <li>{@code reservations}: weekly counts for statuses BOOKED/SEATED/BILL_SENT/PAID/COMPLETED in the current month.</li>
     *   <li>{@code waiting}: weekly counts for statuses WAITING/WAITING_CALLED in the current month.</li>
     *   <li>{@code arrivalDelays}: point-series map where X=order_number and Y=arrival delay minutes.</li>
     *   <li>{@code waitingTimes}: point-series map where X=order_number and Y=waiting minutes
     *       (based on {@code seatedTimeByCode} for the same confirmation_code).</li>
     * </ul>
     * Responds with {@code REPORTS_DATA} and a nested map keyed by:
     * {@code reservations}, {@code waiting}, {@code arrivalDelays}, {@code waitingTimes}.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     * @throws IOException  if sending response fails
     */
	private void handleGetReportsData(Connection conn, ConnectionToClient client) throws SQLException, IOException {

		Map<String, Map<String, Integer>> data = new HashMap<>();

		Map<String, Integer> reservations = new LinkedHashMap<>();
		Map<String, Integer> waiting = new LinkedHashMap<>();

		Map<String, Integer> arrivalDelaysPoints = new LinkedHashMap<>();
		Map<String, Integer> waitingTimePoints = new LinkedHashMap<>();

		String reservationSql = """
				SELECT WEEK(order_date, 1) AS week, COUNT(*) AS total
				FROM `order`
				WHERE order_status IN ('BOOKED','SEATED','BILL_SENT','PAID','COMPLETED')
				  AND MONTH(order_date) = MONTH(CURDATE())
				  AND YEAR(order_date) = YEAR(CURDATE())
				GROUP BY WEEK(order_date, 1)
				ORDER BY WEEK(order_date, 1)
				""";

		try (PreparedStatement ps = conn.prepareStatement(reservationSql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				reservations.put("Week " + rs.getInt("week"), rs.getInt("total"));
			}
		}

		String waitingSql = """
				SELECT WEEK(order_date, 1) AS week, COUNT(*) AS total
				FROM `order`
				WHERE order_status IN ('WAITING','WAITING_CALLED')
				  AND MONTH(order_date) = MONTH(CURDATE())
				  AND YEAR(order_date) = YEAR(CURDATE())
				GROUP BY WEEK(order_date, 1)
				ORDER BY WEEK(order_date, 1)
				""";

		try (PreparedStatement ps = conn.prepareStatement(waitingSql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				waiting.put("Week " + rs.getInt("week"), rs.getInt("total"));
			}
		}

		String pointsSql = """
		        SELECT
		            order_number,
		            order_date,
		            order_time,
		            arrival_datetime
		        FROM `order`
		        WHERE arrival_datetime IS NOT NULL
		          AND MONTH(order_date) = MONTH(CURDATE())
		          AND YEAR(order_date) = YEAR(CURDATE())
		        ORDER BY order_number
		        """;

		try (PreparedStatement ps = conn.prepareStatement(pointsSql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				int orderNumber = rs.getInt("order_number");

				java.sql.Date d = rs.getDate("order_date");
				java.sql.Time t = rs.getTime("order_time");
				java.sql.Timestamp arrivalTs = rs.getTimestamp("arrival_datetime");

				if (d == null || t == null || arrivalTs == null)
					continue;

				java.time.LocalDateTime planned = java.time.LocalDateTime.of(d.toLocalDate(), t.toLocalTime());

				java.time.LocalDateTime actualArrival = arrivalTs.toLocalDateTime();

				long arrivalDelayMin = java.time.Duration.between(planned, actualArrival).toMinutes();

				arrivalDelaysPoints.put(String.valueOf(orderNumber), (int) arrivalDelayMin);


			}
		}
		String waitingTimeSql = """
		        SELECT
		            order_number,
		            TIMESTAMPDIFF(
		                MINUTE,
		                arrival_datetime,
		                status_datetime
		            ) AS waiting_minutes
		        FROM `order`
		        WHERE arrival_datetime IS NOT NULL
		          AND status_datetime IS NOT NULL
		          AND order_status IN ('SEATED', 'WAITING_SEATED', 'BILL_SENT')
		          AND MONTH(order_date) = MONTH(CURDATE())
		          AND YEAR(order_date) = YEAR(CURDATE())
		          AND status_datetime >= arrival_datetime
		        ORDER BY order_number
		        """;

		try (PreparedStatement ps = conn.prepareStatement(waitingTimeSql);
		     ResultSet rs = ps.executeQuery()) {

		    while (rs.next()) {
		        waitingTimePoints.put(
		                String.valueOf(rs.getInt("order_number")),
		                rs.getInt("waiting_minutes")
		        );
		    }
		}

		data.put("reservations", reservations);
		data.put("waiting", waiting);
		data.put("arrivalDelays", arrivalDelaysPoints);
		data.put("waitingTimes", waitingTimePoints);


		client.sendToClient(new Response("REPORTS_DATA", data));
	}

    /**
     * Updates (or inserts) opening hours for a weekday in {@code openhours}.
     * <p>
     * Expects {@link Request#getData()} to be an {@link OpenHours} object.
     * Uses {@code REPLACE INTO} to overwrite existing row by primary key ({@code day}).
     * Responds with {@code OPENING_HOURS_UPDATED}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is {@link OpenHours}
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleSetOpeningHours(Connection conn, Request req, ConnectionToClient client) throws SQLException {

        OpenHours hours = (OpenHours) req.getData();

        System.out.println("=== OPEN HOURS RECEIVED ===");
        System.out.println("day=" + hours.getDay());
        System.out.println("open=" + hours.getOpen());
        System.out.println("close=" + hours.getClose());

        String sql = "REPLACE INTO openhours (day, open, close) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hours.getDay());
            ps.setString(2, hours.getOpen());
            ps.setString(3, hours.getClose());
            ps.executeUpdate();
        }

        try {
            client.sendToClient(new Response("OPENING_HOURS_UPDATED", null));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Creates or updates a special day (closure or limited hours) and cancels affected reservations.
     * <p>
     * Expects {@link Request#getData()} to be a {@code SpecialDayDTO}.
     * Runs in a transaction:
     * <ol>
     *   <li>Upserts into {@code specialdays} using {@code ON DUPLICATE KEY UPDATE}.</li>
     *   <li>Finds affected orders on that date (and time range if partial day).</li>
     *   <li>Updates affected orders to {@code CANCELLED_BY_STAFF} and updates {@code status_datetime=NOW()}.</li>
     * </ol>
     * After commit, sends cancellation emails to affected customers and responds {@code SPECIAL_DAY_SET}.
     * If an exception occurs, rolls back the transaction.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is {@code SpecialDayDTO}
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleSetSpecialDay(Connection conn, Request req, ConnectionToClient client) throws SQLException {

        SpecialDayDTO sd = (SpecialDayDTO) req.getData();
        System.out.println("=== SPECIAL DAY RECEIVED ===");
        System.out.println("date=" + sd.date);

        conn.setAutoCommit(false);

        try {
            String insertSql =
                    "INSERT INTO specialdays (special_date, is_all_day_close, start_time, end_time, reason) " +
                            "VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "is_all_day_close = VALUES(is_all_day_close), " +
                            "start_time = VALUES(start_time), " +
                            "end_time = VALUES(end_time), " +
                            "reason = VALUES(reason)";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setDate(1, java.sql.Date.valueOf(sd.date));
                ps.setBoolean(2, sd.allDayClose);

                if (sd.allDayClose) {
                    ps.setNull(3, Types.TIME);
                    ps.setNull(4, Types.TIME);
                } else {
                    ps.setTime(3, java.sql.Time.valueOf(sd.start));
                    ps.setTime(4, java.sql.Time.valueOf(sd.end));
                }

                ps.setString(5, sd.reason);
                ps.executeUpdate();
            }

            List<String> affectedEmails = new ArrayList<>();

            String selectSql;
            if (sd.allDayClose) {
                selectSql =
                        "SELECT customer_email FROM `order` " +
                                "WHERE order_date = ? " +
                                "AND order_status IN ('BOOKED','WAITING','WAITING_CALLED')";
            } else {
                selectSql =
                        "SELECT customer_email FROM `order` " +
                                "WHERE order_date = ? " +
                                "AND order_time >= ? " +
                                "AND order_time < ? " +
                                "AND order_status IN ('BOOKED','WAITING','WAITING_CALLED')";
            }

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setDate(1, java.sql.Date.valueOf(sd.date));

                if (!sd.allDayClose) {
                    ps.setTime(2, java.sql.Time.valueOf(sd.start));
                    ps.setTime(3, java.sql.Time.valueOf(sd.end));
                }

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    affectedEmails.add(rs.getString("customer_email"));
                }
            }

            String cancelSql;
            if (sd.allDayClose) {
                cancelSql =
                        "UPDATE `order` SET order_status='CANCELLED_BY_STAFF', status_datetime=NOW() " +
                                "WHERE order_date = ? " +
                                "AND order_status IN ('BOOKED','WAITING','WAITING_CALLED')";
            } else {
                cancelSql =
                        "UPDATE `order` SET order_status='CANCELLED_BY_STAFF', status_datetime=NOW() " +
                                "WHERE order_date = ? " +
                                "AND order_time >= ? " +
                                "AND order_time < ? " +
                                "AND order_status IN ('BOOKED','WAITING','WAITING_CALLED')";
            }

            try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                ps.setDate(1, java.sql.Date.valueOf(sd.date));

                if (!sd.allDayClose) {
                    ps.setTime(2, java.sql.Time.valueOf(sd.start));
                    ps.setTime(3, java.sql.Time.valueOf(sd.end));
                }

                int cancelled = ps.executeUpdate();
                System.out.println("Cancelled orders: " + cancelled);
            }

            conn.commit();

            for (String email : affectedEmails) {
                EMAIL_EXECUTOR.submit(() -> {
                    try {
                        EmailService.StaffCancelSDEmail(email);
                    } catch (Exception ignored) {
                    }
                });
            }

            client.sendToClient(new Response("SPECIAL_DAY_SET", null));

        } catch (Exception e) {
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Deletes a special day entry for a given date.
     * <p>
     * Expects {@link Request#getData()} to be a {@link LocalDate}.
     * Deletes from {@code specialDays} by {@code special_date} and responds {@code SPECIAL_DAY_DELETED}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request whose data is the date ({@link LocalDate})
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     */
    private void handleDeleteSpecialDay(Connection conn, Request req, ConnectionToClient client) throws SQLException {
        System.out.println("=== SPECIAL DAY DELETED ===");
        LocalDate date = (LocalDate) req.getData();
        System.out.println("date=" + date);

        String sql = "DELETE FROM specialDays WHERE special_date = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.executeUpdate();
        }

        try {
            client.sendToClient(new Response("SPECIAL_DAY_DELETED", null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all currently active reservations (various non-final statuses) and sends them to the client.
     * <p>
     * Returns {@code ACTIVE_RESERVATIONS_LIST} with a list of {@link Order} objects containing
     * the selected fields (others are left {@code 0}/{@code null}).
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     * @throws IOException  if sending response fails
     */
    private void handleGetActiveReservations(
            Connection conn,
            ConnectionToClient client) throws SQLException, IOException {

        List<Order> list = new ArrayList<>();

        String sql = """
                SELECT order_number, order_date, order_time,
                       number_of_guests, order_status, table_num
                FROM `order`
                WHERE order_status IN (

                    'SEATED',
                    'BILL_SENT'
                )
                ORDER BY order_date, order_time
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        rs.getInt("table_num"),
                        rs.getString("order_status"),
                        null,
                        false,
                        null
                ));
            }

        }

        client.sendToClient(
                new Response("ACTIVE_RESERVATIONS_LIST", list)
        );
    }

    /**
     * Retrieves current customers inside the restaurant and sends them to the client.
     * <p>
     * Selects orders in statuses {@code SEATED} or {@code WAITING_SEATED} and returns
     * {@code CURRENT_CUSTOMERS_LIST} with a list of {@link Order} objects containing customer info and guest count.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     * @throws IOException  if sending response fails
     */
    private void handleGetCurrentCustomers(
            Connection conn,
            ConnectionToClient client) throws SQLException, IOException {

        List<Order> list = new ArrayList<>();

        String sql =
                "SELECT customer_name, customer_email, customer_phone, number_of_guests " +
                        "FROM `order` " +
                        "WHERE order_status IN ('SEATED','WAITING_SEATED')";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int guests = rs.getInt("number_of_guests");

                list.add(new Order(
                        0,
                        null,
                        null,
                        guests,
                        0,
                        0,
                        rs.getString("customer_name"),
                        rs.getString("customer_email"),
                        rs.getString("customer_phone"),
                        null,
                        0,
                        null,
                        null,
                        false,
                        null
                ));
            }

        }

        client.sendToClient(new Response("CURRENT_CUSTOMERS_LIST", list));
    }

    /**
     * Retrieves the current waiting list (waiting/called) and sends it to the client.
     * <p>
     * Expects a {@link Request} (data not used).
     * Returns {@code WAITING_LIST} with a list of {@link Order} objects where
     * {@code order_status} is mapped to {@code WAITING} / {@code CALLED} for display.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    request (not used)
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleGetWaitingList(Connection conn, Request req, ConnectionToClient client) throws Exception {

        List<Order> list = new ArrayList<>();

        String sql = """
                SELECT order_number, number_of_guests, order_time, order_date
                FROM `order`
                WHERE order_status IN ('WAITING','WAITING_CALLED')
                ORDER BY TIMESTAMP(order_date,order_time)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            String status = "";
            while (rs.next()) {

                if (rs.getString("order_status").equals("WAITING_CALLED"))
                    status = "CALLED";
                else
                    status = "WAITING";

                list.add(new Order(
                        rs.getInt("order_number"),
                        rs.getString("order_date"),
                        rs.getString("order_time"),
                        rs.getInt("number_of_guests"),
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        0,
                        status,
                        null,
                        false,
                        null
                ));
            }

        }
        client.sendToClient(new Response("WAITING_LIST", list));
    }

    /**
     * Authenticates staff (manager/agent/representative) using username and password.
     * <p>
     * On success: sends {@code STAFF_LOGIN_SUCCESS} with a {@link Staff} object.
     * On failure: sends {@code LOGIN_FAIL}.
     * </p>
     *
     * @param conn   active DB connection
     * @param req    staff login request with username and password
     * @param client client connection to respond to
     * @throws Exception if DB or client communication fails
     */
    private void handleStaffLogin(
            Connection conn,
            StaffLoginRequest req,
            ConnectionToClient client) throws Exception {

        String sql = """
                SELECT staff_id, username, role
                FROM staff
                WHERE username = ? AND password = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, req.getUsername());
            ps.setString(2, req.getPassword());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Staff staff = new Staff(
                        rs.getInt("staff_id"),
                        rs.getString("username"),
                        rs.getString("role")
                );

                client.sendToClient(
                        new Response("STAFF_LOGIN_SUCCESS", staff)
                );
            } else {
                client.sendToClient(
                        new Response("LOGIN_FAIL", null)
                );
            }
        }
    }

    /**
     * Logs a subscriber in using an identifier provided by QR (string id).
     * <p>
     * Queries {@code subscriber} by {@code subscriber_id}. On success sends {@code SUBSCRIBER_LOGIN_SUCCESS}
     * with a {@link Subscriber}; otherwise sends {@code SUBSCRIBER_LOGIN_FAIL}.
     * </p>
     *
     * @param conn   active DB connection
     * @param id     subscriber id as string
     * @param client client connection to respond to
     * @throws Exception if parsing, DB, or client communication fails
     */
    private void handleLoginWithQR(
            Connection conn,
            String id,
            ConnectionToClient client) throws Exception {

        String sql = "SELECT * FROM subscriber WHERE subscriber_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Subscriber sub = new Subscriber(
                        rs.getInt("subscriber_id"),
                        rs.getString("subscriber_name"),
                        rs.getString("subscriber_email"),
                        rs.getString("subscriber_phone")
                );

                client.sendToClient(
                        new Response("SUBSCRIBER_LOGIN_SUCCESS", sub)
                );
            } else {
                client.sendToClient(
                        new Response("SUBSCRIBER_LOGIN_FAIL", null)
                );
            }
        }
    }

    /**
     * Retrieves all subscribers and sends them to the client.
     * <p>
     * Responds with {@code SUBSCRIBERS_LIST} and a list of {@link Subscriber} objects.
     * </p>
     *
     * @param conn   active DB connection
     * @param client client connection to respond to
     * @throws SQLException if DB operations fail
     * @throws IOException  if sending response fails
     */
    private void handleGetAllSubscribers(Connection conn, ConnectionToClient client)
            throws SQLException, IOException {

        List<Subscriber> list = new ArrayList<>();

        String sql = """
                SELECT subscriber_id, subscriber_name, subscriber_email, subscriber_phone
                FROM subscriber
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Subscriber(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ));
            }
        }

        client.sendToClient(new Response("SUBSCRIBERS_LIST", list));
    }

    /**
     * Finds a free physical table of the given capacity for the current day and current time.
     * <p>
     * Returns the first table that matches {@code places} and is not currently occupied by an active
     * order (statuses: SEATED/WAITING/WAITING_SEATED/BILL_SENT/WAITING_CALLED) within a 2-hour window.
     * </p>
     *
     * @param conn   active DB connection
     * @param places required table capacity
     * @return available table number, or {@code null} if none is available
     * @throws SQLException if DB operations fail
     */
    private Integer findFreePhysicalTableNow(Connection conn, int places) throws SQLException {

        String tableSql = "SELECT t.table_num " + "FROM tables t " + "WHERE t.places=? " + "AND NOT EXISTS ( "
                + "   SELECT 1 FROM `order` o " + "   WHERE o.table_num = t.table_num "
                + "   AND o.table_num IS NOT NULL " + "   AND o.order_date = CURDATE() "
                + "   AND o.order_status IN ('SEATED','WAITING','WAITING_SEATED','BILL_SENT','WAITING_CALLED') " + "   AND TIME(NOW()) < ADDTIME(o.order_time,'02:00:00') " + ") "
                + "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(tableSql)) {
            ps.setInt(1, places);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            return null;
        }
    }

    /**
     * Finds a free physical table of the given capacity for a specific date.
     * <p>
     * Returns the first table that matches {@code places} and is not occupied by an active
     * order (statuses: SEATED/WAITING/WAITING_SEATED/BILL_SENT/WAITING_CALLED) within a 2-hour window.
     * </p>
     *
     * @param conn   active DB connection
     * @param places required table capacity
     * @param date   reservation date to check
     * @return available table number, or {@code null} if none is available
     * @throws SQLException if DB operations fail
     */
    private Integer findFreePhysicalTable(Connection conn, int places, LocalDate date, LocalTime time) throws SQLException {

        // Compute requested start and end as full datetime
        LocalDateTime requestedStart = LocalDateTime.of(date, time);
        LocalDateTime requestedEnd = requestedStart.plusHours(2);

        String tableSql = """
            SELECT t.table_num
            FROM tables t
            WHERE t.places = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM `order` o
                  WHERE o.table_num = t.table_num
                    AND o.table_num IS NOT NULL
                    AND o.order_status IN ('SEATED','WAITING','WAITING_SEATED','BILL_SENT','WAITING_CALLED')
                    AND o.arrival_datetime < ?                  -- existing order start < requested end
                    AND DATE_ADD(o.arrival_datetime, INTERVAL 2 HOUR) > ? -- existing order end > requested start
              )
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(tableSql)) {
            ps.setInt(1, places);                              // t.places = ?
            ps.setString(2, requestedEnd.toString());          // existing start < requested end
            ps.setString(3, requestedStart.toString());        // existing end > requested start

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1); // return available table number
            }
            return null; // no table available
        }
    }


    /**
     * Hook called when a new client connects to the server.
     * <p>
     * Stores the client IP address in {@code clientIPs} and logs the connection.
     * </p>
     *
     * @param client the connected client
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        clientIPs.put(client, client.getInetAddress().getHostAddress());
        System.out.println("[Client] Connected: " + client.getInetAddress());
    }

    /**
     * Hook called when a client disconnects.
     * <p>
     * Removes the disconnected client from {@code activeSubscribers} (logout) and logs the event.
     * </p>
     *
     * @param client the disconnected client
     */
    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        activeSubscribers.entrySet().removeIf(entry -> {
            if (entry.getValue() == client) {
                System.out.println("[LOGOUT] Subscriber " + entry.getKey() + " logged out");
                return true;
            }
            return false;
        });
    }

    /**
     * Hook called when a client disconnects due to an exception.
     * <p>
     * Logs the exception stack trace for debugging.
     * </p>
     *
     * @param client    the client that caused the exception
     * @param exception the thrown exception
     */
    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        System.out.println("[Client] Disconnected due to exception:");
        exception.printStackTrace();
    }
}