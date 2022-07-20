package py.monitor.jmx.server;

/**
 * we use this enum to define resource types,such as : volume,storage pool,group and so on
 * 
 * @author sxl
 * 
 * @caution DO NOT ADD METHOD LIKE : "findValueByName", THE BASE METHOD "valueOf" FROM CLASS java.lang.Enum CAN DO THE
 *          SAME JOB AS "findValueByName"
 *
 */
public enum ResourceType {
    /**
     * not a resource
     */
    NONE,

    /**
     * volume level resource
     */
    VOLUME,

    /**
     * storage pool level resource
     */
    STORAGE_POOL,

    /**
     * storage level resource
     */
    STORAGE,

    /**
     * 
     */
    GROUP,

    /**
     * machine level resource
     */
    MACHINE,

    /**
     * java virtual machine level resource
     */
    JVM,

    /**
     * network resource
     */
    NETWORK,

    /**
     * disk level resource, we can use archive as disk.
     */
    DISK;

    public static String getRegex() {
        String regEx = "(";
        for (ResourceType resourceType : values()) {
            regEx += resourceType.name();
            regEx += "|";
        }
        regEx = regEx.substring(0, regEx.length() - 1);
        regEx += ")";
        return regEx;
    }
}
