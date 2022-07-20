package py.monitor.alarmbak;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.IllegalIndexException;

/**
 * the importance of alarm-level is : CRITICAL > MAJOR > MINOR > WARNING,
 * 
 * @author shixulu
 *
 */
public enum AlarmLevel implements Comparable<AlarmLevel>, Serializable {
    /**
     * 
     */
    CRITICAL(1),
    /**
     * 
     */
    MAJOR(2),
    /**
     * 
     */
    MINOR(3),
    /**
     * 
     */
    WARNING(4);

    private static final Logger logger = LoggerFactory.getLogger(AlarmLevel.class);

    private final int value;

    private AlarmLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AlarmLevel get(int portTypeIndex) throws IllegalIndexException {
        for (AlarmLevel portType : values()) {
            if (portTypeIndex == portType.getValue()) {
                return portType;
            }
        }

        logger.error("The index : {} is out of bound");
        throw new IllegalIndexException();
    }

}
