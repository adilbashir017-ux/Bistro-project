package logic;

import java.io.Serializable;

/**
 * Represents a payment history record.
 * <p>
 * This class stores information about a past payment, including
 * the payment identifier, payment date, amount, and payment status.
 * It is typically used as a Data Transfer Object (DTO) for displaying
 * payment history to users or administrators.
 * </p>
 */
public class PaymentHistory implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the payment */
    private int paymentId;

    /** The date on which the payment was made */
    private String date;

    /** The amount paid */
    private double amount;

    /** The status of the payment */
    private String status;

    /**
     * Constructs a {@code PaymentHistory} object.
     * Initializes the payment history details.
     *
     * @param paymentId the unique identifier of the payment
     * @param date      the date of the payment
     * @param amount    the payment amount
     * @param status    the status of the payment
     */
    public PaymentHistory(int paymentId, String date, double amount, String status) {
        this.paymentId = paymentId;
        this.date = date;
        this.amount = amount;
        this.status = status;
    }

    /**
     * Returns the payment ID.
     *
     * @return the payment ID
     */
    public int getPaymentId() {
        return paymentId;
    }

    /**
     * Returns the payment date.
     *
     * @return the payment date
     */
    public String getDate() {
        return date;
    }

    /**
     * Returns the payment amount.
     *
     * @return the payment amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the payment status.
     *
     * @return the payment status
     */
    public String getStatus() {
        return status;
    }
}