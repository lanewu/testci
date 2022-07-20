package py.instance;

/**
 * represent the status of instance
 */

public enum InstanceStatus {
    OK(1), // instance work normally
    SUSPEND(2), // instance don`t work, but prepare to supply the service
    INC(3), // instance work incorrectly
    FAILED(4), // instance has been failed by user manually
    FORGOTTEN(5); // failed instance will be moved to FORGOTTEN after a while

    private final int value;

    private InstanceStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Compare the instance status with another instance status.
     * 
     * @param other
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     */
    public int compareWith(InstanceStatus other) {
        if (this.value > other.value) {
            return 1;
        } else if (this.value < other.value) {
            return -1;
        } else {
            return 0;
        }
    }

    public static InstanceStatus findByValue(int value) {
        for (InstanceStatus status : values()) {
            if (value == status.value) {
                return status;
            }
        }

        return null;
    }
}
