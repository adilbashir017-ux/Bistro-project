package logic;

import java.io.Serializable;

/**
 * Represents a generic request object.
 * <p>
 * This class is used to encapsulate a request status along with
 * its associated data and optional additional data.
 * It is commonly used as a Data Transfer Object (DTO) between
 * different layers of the system.
 * </p>
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private Object data;
    private Object extraData;

    /**
     * Constructs a {@code Request} object.
     * Initializes the request status and data.
     *
     * @param status the status of the request
     * @param data   the data associated with the request
     */
    public Request(String status, Object data) {
        this.status = status;
        this.data = data;
        this.extraData = null;
    }

    /**
     * Constructs a {@code Request} object.
     * Initializes the request status, main data, and additional data.
     *
     * @param status    the status of the request
     * @param data      the main data associated with the request
     * @param extraData additional data associated with the request
     */
    public Request(String status, Object data, Object extraData) {
        this.status = status;
        this.data = data;
        this.extraData = extraData;
    }
    /**
     * Returns the status of the request.
     *
     * @return the request status
     */
    public String getStatus() {
        return status;
    }
    /**
     * Sets the status of the request.
     *
     * @param status the request status
     */
    public void setStatus(String status) {
        this.status = status;
    }
    /**
     * Returns the main data associated with the request.
     *
     * @return the request data
     */
    public Object getData() {
        return data;
    }
    /**
     * Sets the main data associated with the request.
     *
     * @param data the request data
     */
    public void setData(Object data) {
        this.data = data;
    }
    /**
     * Returns the additional data associated with the request.
     *
     * @return the additional request data
     */
    public Object getExtraData() {
        return extraData;
    }
    /**
     * Sets the additional data associated with the request.
     *
     * @param extraData the additional request data
     */
    public void setExtraData(Object extraData) {
        this.extraData = extraData;
    }
}