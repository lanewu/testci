package py.processmanager.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * utils of process
 * 
 * @author liy
 * 
 */
public class PMUtils {

    private static final Log logger = LogFactory.getLog(PMUtils.class);

    private final static String LAUNCH_FAILED = "failed";

    private final static String LAUNCH_SUCCEEDED = "succeeded";

    /**
     * judge a process whether active
     * 
     * @param process
     * @return
     */
    public static boolean isActive(Process process) {
        if (process == null) {
            return false;
        }
        try {
            process.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Output a failure string to the system out so that whoever launching this
     * driver can get it.
     */
    public static void outputFailure() {
        // print out an empty line in case any one before one print some thing
        // without line break;
        System.out.println();
        System.out.println(LAUNCH_FAILED);
    }

    /**
     * Output a ok string to the system out so that whoever launching this
     * driver can get it.
     */
    public static void outputOk() {
        // print out an empty line in case any one before one print some thing
        // without line break;
        System.out.println();
        System.out.println(LAUNCH_SUCCEEDED);
    }

    public static boolean processStartResults(BufferedReader reader) {
        if (reader == null) {
            logger.error("Reader is null");
            return false;
        }

        String result = null;
        boolean succeeded = false;
        do {
            try {
                logger.debug("reading the processing result of process start");
                result = reader.readLine();
            } catch (IOException e) {
                logger.error("Failed to read process builder results", e);
                break;
            }

            if (result == null) {
                logger.error("the start results can't be null before \"failed\" or \"succeeded\" strings are returned");
                break;
            }

            if (result.equals(LAUNCH_SUCCEEDED)) {
                logger.debug("Successfully start process");
                succeeded = true;
                break;
            } else if (result.equals(LAUNCH_FAILED)) {
                logger.error("Failed to start process");
                break;
            } else {
                logger.warn("the process builder returns unknown string:" + result);
                // continue;
            }
        } while (true);

        // close reader
        try {
            reader.close();
        } catch (IOException e) {
            logger.warn("can't close the input reader reading results from sub-process", e);
        }
        return succeeded;
    }

    /**
     * get current jvm process pid
     * 
     * @return
     */
    public static int getCurrentProcessPid() {
        String pidStr = ManagementFactory.getRuntimeMXBean().getName();
        if (pidStr.split("@").length == 2) {
            return Integer.valueOf(pidStr.split("@")[0]);
        }
        return 0;
    }

    /**
     * get process pid by process
     * 
     * @param process
     * @return
     */
    public static int getProcessPidByProcess(Process process) {
        int pid = 0;
        if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
            try {
                Field field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                pid = field.getInt(process);
            } catch (Throwable e) {
                logger.error("failed to get process pid", e);
            }
        }
        return pid;
    }

    /**
     * check whether the process active by process itself
     * 
     * @param process
     * @return
     */
    public static boolean isProcessActive(Process process) {
        if (process != null) {
            try {
                process.exitValue();
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /**
     * check whether the process active by process pid
     * 
     * @param pid
     * @return
     */
    public static boolean isProcessActive(int pid) {
        String command = "ps -p " + pid;
        logger.debug(command);
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = stdInput.readLine();
            while(line != null) {
                line = stdInput.readLine();
                logger.debug(line);

                if(line != null && line.contains(String.valueOf(pid))) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("Caught an exception when waiting for process to be shutdown", e);
        }
        return false;
    }

}
