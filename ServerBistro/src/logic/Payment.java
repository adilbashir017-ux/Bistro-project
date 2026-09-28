package logic;

public class Payment {

    private int payment_id;
    private int order_number;
    private double amount;
    /**
     * Constructs a {@code Payment} object.
     * Initializes the payment details associated with an order.
     *
     * @param payment_id   the unique identifier of the payment
     * @param order_number the order number associated with the payment
     * @param amount       the payment amount
     */
    public Payment(int payment_id, int order_number, double amount) {
        super();
        this.payment_id = payment_id;
        this.order_number = order_number;
        this.amount = amount;
    }
    /**
     * Returns the payment ID.
     *
     * @return the payment ID
     */
    public int getPayment_id() {
        return payment_id;
    }
    /**
     * Sets the payment ID.
     *
     * @param payment_id the payment ID
     */
    public void setPayment_id(int payment_id) {
        this.payment_id = payment_id;
    }
    /**
     * Returns the order number associated with this payment.
     *
     * @return the order number
     */
    public int getOrder_number() {
        return order_number;
    }
    /**
     * Sets the order number associated with this payment.
     *
     * @param order_number the order number
     */
    public void setOrder_number(int order_number) {
        this.order_number = order_number;
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
     * Sets the payment amount.
     *
     * @param amount the payment amount
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }
    /**
     * Returns a string representation of the payment.
     *
     * @return a string representation of the payment
     */
    @Override
    public String toString() {
        return "Payment [payment_id=" + payment_id + ", order_number=" + order_number + ", amount=" + amount + "]";
    }



}