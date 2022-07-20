package py.monitor.pojo.management;

/**
 * this interface will be used to extend the unit of measurement enum
 * 
 * @author shixulu
 *
 */
public enum UnitOfMeasurement {
    /**
     * basic
     */
    NONE,

    /**
     * time
     */
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MINISECOND,

    /**
     * storage
     */
    BIT, BYTE, KB, MB, TB, ZB,

    /**
     * percentage
     */
    PERCENTAGE_OF_10, PERCENTAGE_OF_100, PERCENTAGE_OF_1000, PERCENTAGE_OF_10000;
}