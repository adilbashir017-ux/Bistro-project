package logic;

import java.io.Serializable;
import java.util.List;



/**
 * Represents a response object containing a user's account history.
 * <p>
 * This class includes the user's orders and visit history and is used
 * as a Data Transfer Object (DTO) between system layers.
 * </p>
 */
public class AccountHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Order> orders;
    private final List<VisitHistory> visits;


    /**
     * Constructs an {@code AccountHistoryResponse} object.
     * Initializes the user's orders and visit history lists.
     *
     * @param orders the list of orders made by the user
     * @param visits the list of the user's visit history records
     */
    public AccountHistoryResponse(List<Order> orders, List<VisitHistory> visits) {
        this.orders = orders;
        this.visits = visits;
    }
    /**
     * Returns the list of orders made by the user.
     *
     * @return the list of orders
     */
    public List<Order> getOrders() {
        return orders;
    }
    /**
     * Returns the user's visit history.
     *
     * @return the list of visit history records
     */
    public List<VisitHistory> getVisits() {
        return visits;
    }
}