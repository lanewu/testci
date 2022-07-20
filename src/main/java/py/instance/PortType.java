package py.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.IllegalIndexException;

/**
 * All instance types in {@code PyService} are: <br>
 * CONSOLE, CONTROLCENTER, INFOCENTER, MONITORCENTER, DATANODE, DRIVERCONTAINER, DIH, COORDINATOR<br>
 * 
 * Each instance takes ports as follows:<br>
 * <code>
 * +-----------------+--------------------------------------+
 * | instance type   | contains ports                       |
 * +-----------------+--------------------------------------+
 * | CONSOLE         | CONTROL                              |
 * +-----------------+--------------------------------------+
 * | CONTROLCENTER   | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * | INFOCENTER      | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * | MONITORCENTER   | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * | DATANODE        | CONTROL & MONITOR & IO  & HEARTBEAT  |
 * +-----------------+--------------------------------------+
 * | DRIVERCONTAINER | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * | DIH             | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * | COORDINATOR     | CONTROL & MONITOR                    |
 * +-----------------+--------------------------------------+
 * </code> <br>
 * 
 * @author shixulu
 *
 */
public enum PortType {
    /**
     * means port is used for control stream
     */
    CONTROL(0),

    /**
     * means port is used for heart beat stream
     */
    HEARTBEAT(1),

    /**
     * means port is used for data IO stream
     */
    IO(2),

    /**
     * means port is used for monitor stream
     */
    MONITOR(3);

    private static final Logger logger = LoggerFactory.getLogger(PortType.class);

    private final int value;

    private PortType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PortType get(int portTypeIndex) throws IllegalIndexException {
        for (PortType portType : values()) {
            if (portTypeIndex == portType.getValue()) {
                return portType;
            }
        }

        logger.error("The port type index : {} is out of bound");
        throw new IllegalIndexException();
    }
}
