package py.processmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import py.processmanager.exception.UnableToStartServiceException;
import py.processmanager.utils.PMUtils;

/**
 * A facility that manage a service process.
 * 
 * Service such as control center was started by process manager in a new process. And when the service is not terminal
 * by shutdown command, the process manager will restart the service.
 * 
 * If a shutdown command send, it will stop process manager first and then the service.
 * 
 * @author liy
 * @modify zjm
 * 
 */
public class ProcessManager {
    public static final String KW_DISABLE = "pm_disabled";

    private static final Logger logger = Logger.getLogger(ProcessManager.class);

    /*
     * path of service launching script
     */
    private String launcherPath;
    private boolean disabled = false;
    private List<String> launcherParams = new ArrayList<String>();

    public String getLauncherPath() {
        return launcherPath;
    }

    public void setLauncherPath(String launcherPath) {
        this.launcherPath = launcherPath;
    }

    public void addParams(String param) {
        launcherParams.add(param);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * this method proguard service running in specified path, always start it as long as the service is dead
     * 
     * @throws UnableToStartServiceException
     */
    public void startService() throws UnableToStartServiceException {
        if (launcherPath == null) {
            logger.error("unable to start service due to launcherPath is not specified");
            throw new UnableToStartServiceException();
        }

        File launcher = new File(launcherPath);
        if (!launcher.exists()) {
            logger.error("unable to start service due to launcher path doesn't exist");
            throw new UnableToStartServiceException();
        }

        String serviceRunningPath = launcher.getParentFile().getParentFile().getAbsolutePath();

        /*
         * backup my process id into a file to let system know me
         */
        boolean alreadyBackupPid = false;
        try {
            PMDB pmdb = PMDB.build(Paths.get(serviceRunningPath));
            String backupStr = String.valueOf(PMUtils.getCurrentProcessPid());
            alreadyBackupPid = pmdb.save(PMDB.PM_PID_NAME, backupStr);
        } catch (Exception e) {
            logger.error("Caught an exception", e);
        }
        if (!alreadyBackupPid) {
            logger.error("cannot backup my process manager pid into a file");
            throw new UnableToStartServiceException();
        }

        logger.debug("Process manager start to do its job, proguard service in " + serviceRunningPath + " running ...");
        while (true) {
            try {
                // append parameters to launch command
                StringBuilder commandBuilder = new StringBuilder();
                commandBuilder.append(launcherPath);
                if (launcherParams.size() > 0) {
                    for (String launcherParam : launcherParams) {
                        commandBuilder.append(" " + launcherParam);
                    }
                }

                String command = commandBuilder.toString();
                // logger.debug("execute cmd " + command + " to start service");
                Process process = Runtime.getRuntime().exec(command, null, new File(serviceRunningPath));
                process.waitFor();

                if (disabled) {
                    logger.warn("Disabled flag detected, process manager exit");
                    break;
                }

            } catch (Exception e) {
                logger.warn("Caught an exception when process manager start service", e);
            }
        }
    }

    /**
     * main method for process manager, service command is required to start process manager
     * 
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String args[]) throws IOException {

        if (args == null || args.length == 0) {
            logger.error("one argument containing service command is required, but cannot get it");
            System.exit(-1);
        }

        ProcessManager processManager = new ProcessManager();

        String launcherPath = args[0];
        processManager.setLauncherPath(launcherPath); // set script path
        logger.info(launcherPath);
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            if (ProcessManager.KW_DISABLE.equals(arg.toLowerCase())) {
                processManager.setDisabled(true);
                continue;
            }

            processManager.addParams(arg);
        }

        try {
            String mutexTargetDir = args[1];
            if (ProcessManagerMutex.checkIfAlreadyRunning(mutexTargetDir)) {
                processManager.startService();
            } else {
                logger.warn("exit due to the same process is processing ");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Caught an exception", e);
            System.exit(1);
        }

    }
}
