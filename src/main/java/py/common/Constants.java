package py.common;

public class Constants {
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String DIH_INSTANCE_NAME = "DIH";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String DATANODE_INSTANCE_NAME = "DataNode";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String CONSOLE_INSTANCE_NAME = "Console";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String INFO_CENTER_INSTANCE_NAME = "InfoCenter";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String DRIVER_CONTAINER_INSTANCE_NAME = "DriverContainer";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String CONTROL_CENTER_INSTANCE_NAME = "ControlCenter";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String SCRIPT_CONTAINER_INSTANCE_NAME = "ScriptContainer";
    /**
     * {@link PyService} is encouraged
     */
    @Deprecated
    public final static String COORDINATOR_INSTANCE_NAME = "Coordinator";

    public final static long SUPERADMIN_ACCOUNT_ID = 1862755152385798543L;
    public final static String SUPERADMIN_DEFAULT_ACCOUNT_NAME = "admin";
    public final static String SUPERADMIN_DEFAULT_ACCOUNT_PASSWORD = "admin";
    public final static String SUPERADMIN_ACCOUNT_TYPE = "SuperAdmin";
    public final static int SECTOR_SIZE = 512;
    public final static String DEFAULT_PASSWORD = "admin";
    /**
     * nbd-client queue size is 2048, when nbd-client is disconnected, still has 2048 requests need to be handle,
     * consider requests are all write type, and each byte can be build to one log, one requests is 128K at max, above
     * this, we can calc max logs number
     */
    public final static long INCREMENT_NUM_OF_LOGS_ONE_CONNECTION = 4096 * 128 * 1024 * 1024L;

    public static int DEPLOYMENT_DAEMON_PORT = 10002;
    public static int CONTROL_CENTER_PORT = 8010;
    public static int INFO_CENTER_PORT = 8020;
    public static int DIH_PORT = 10000;
    public static int CONSOLE_PORT = 8080;
    public static int DATANODE_PORT = 10011;
    public static int MONITOR_CENTER_PORT = 11000;
    public static int DRIVER_CONTAINER_PORT = 9000;
}
