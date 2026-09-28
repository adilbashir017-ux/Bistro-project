package logic;

import java.io.Serializable;

/**
 * Represents a generic response object.
 * <p>
 * This class is used to encapsulate a response status along with
 * its associated data. It is commonly used for communication
 * between different layers of the system.
 * </p>
 */
public class Response implements Serializable {

    /** Serial version UID for serialization compatibility */
    private static final long serialVersionUID = 1L;

    /** The status of the response */
    private String status;

    /** The data associated with the response */
    private Object data;

    /**
     * Constructs a {@code Response} object.
     * Initializes the response status and associated data.
     *
     * @param status the status of the response
     * @param data   the data associated with the response
     */
    public Response(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    /**
     * Returns the status of the response.
     *
     * @return the response status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the response.
     *
     * @param status the response status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the data associated with the response.
     *
     * @return the response data
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the data associated with the response.
     *
     * @param data the response data
     */
    public void setData(Object data) {
        this.data = data;
    }
}