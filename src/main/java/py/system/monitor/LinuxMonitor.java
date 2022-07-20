package py.system.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance of this class load info of linux processes to memory for pengyun service process management.
 * <p>
 * In linux, process info stored in directory ''/proc/[pid]'', we load all these files content in memory by specific
 * pattern.
 * 
 * @author zjm
 */
public class LinuxMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(LinuxMonitor.class);

    /**
     * Use this pattern to parse pid from files.
     */
    private static final Pattern PID_PATTERN = Pattern.compile("([\\d]*).*");
    /**
     * Use this pattern to parse os release from files.
     */
    private static final Pattern DISTRIBUTION = Pattern.compile("DISTRIB_DESCRIPTION=\"(.*)\"", Pattern.MULTILINE);

    private FileUtils fileUtils;

    LinuxMonitor(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    public LinuxMonitor() {
        fileUtils = new FileUtils();
    }

    @Override
    public String osName() {
        String distribution = fileUtils.runRegexOnFile(DISTRIBUTION, "/etc/lsb-release");
        if (null == distribution) {
            return System.getProperty("os.name");
        }
        return distribution;
    }

    @Override
    public int currentPid() {
        String pid = fileUtils.runRegexOnFile(PID_PATTERN, "/proc/self/stat");
        return Integer.parseInt(pid);
    }

    @Override
    public ProcessInfo[] processTable() {
        ArrayList<ProcessInfo> processTable = new ArrayList<>();
        final String[] pids = fileUtils.pidsFromProcFilesystem();
        for (int i = 0; i < pids.length; i++) {
            ProcessInfo processInfo = processInfo(Integer.valueOf(pids[i]));

            if (processInfo != null)
                processTable.add(processInfo);
        }
        return processTable.toArray(new ProcessInfo[processTable.size()]);
    }

    @Override
    public void killProcess(int pid) {
        try {
            Process process = Runtime.getRuntime().exec("kill -9 " + pid);
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Could not kill process id " + pid, e);
        }
    }

    @Override
    public ProcessInfo processInfo(int pid) {
        try {
            String stat = fileUtils.slurp("/proc/" + pid + "/stat");
            String status = fileUtils.slurp("/proc/" + pid + "/status");
            String cmdline = fileUtils.slurp("/proc/" + pid + "/cmdline");
            String cwd = fileUtils.realPath("/proc/" + pid + "/cwd");
            int nFDs = fileUtils.numberOfSubFiles("/proc/" + pid + "/fd");
            int nTasks = fileUtils.numberOfSubFiles("/proc/" + pid + "/task");
            UnixPasswdParser passwdParser = new UnixPasswdParser();
            final LinuxProcessInfoParser parser = new LinuxProcessInfoParser(stat, status, cmdline,
                    passwdParser.parse(), cwd, nFDs, nTasks);
            return parser.parse();
        } catch (ParseException pe) {
            // Skip this process, but log a warning for diagnosis.
            logger.warn("Caught an exception when parse process info of PID:{} from file system", pid, pe);
        } catch (IOException ioe) {
            // process probably died since we got the process list
            logger.info("Caught an exception when parse process info of PID:{} from file system. detail: {}", pid, ioe);
        }

        return null;
    }
}
