package py.system.monitor;

/**
 * This object represents LinuxMonitor's understanding of a process. You can get all the information LinuxMonitor knows
 * about a particular process from this object.
 * <p>
 * There are also convenience methods that can be used to print out a process table in a monospace font (for example, on
 * the console).
 */
public class ProcessInfo {

    // Process id info
    private int pid;
    private int parentPid;
    private String command;
    private String name;
    private String owner;
    private String cwd;
    private int nFDs;
    private int nTasks;

    public ProcessInfo(int pid, int parentPid, String command, String name, String owner, String cwd, int nFDs,
            int nTasks) {
        this.pid = pid;
        this.parentPid = parentPid;
        this.command = command;
        this.name = name;
        this.owner = owner;
        this.cwd = cwd;
        this.nFDs = nFDs;
        this.nTasks = nTasks;
    }

    /**
     * The id of this process
     *
     * @return The id of this process
     */
    public int getPid() {
        return pid;
    }

    /**
     * The id of the parent process of this parent
     *
     * @return The id of the parent process of this parent
     */
    public int getParentPid() {
        return parentPid;
    }

    /**
     * The command that was originally used to start this process. Not currently available on Mac OSX or Solaris (the C
     * source contains some information on how to get this data for anyone interested in implementing it)
     *
     * @return A string representing the command that was originally used to start this process.
     */
    public String getCommand() {
        return command;
    }

    /**
     * The name of this process. This is for display purposes only.
     *
     * @return The name of this process.
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the owner of this process. This is derived from the uid, not the effective id. On Windows, this is in
     * the format DOMAIN\USERNAME
     *
     * @return The name of the owner of this process.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * The current working directory of this process.
     * 
     * @return The current working directory of this process.
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * The number of file descriptors being held by this process.
     * 
     * @return The number of file descriptors being held by this process.
     */
    public int getnFDs() {
        return nFDs;
    }

    /**
     * The number of tasks(threads) of this process.
     * 
     * @return The number of tasks(threads) of this process.
     */
    public int getnTasks() {
        return nTasks;
    }

    /**
     * A one-line string representation of some of the information in this object that can be used to print out a single
     * line in a process table. Fields have a fixed length so that the table looks nice in a monospace font.
     *
     * @return a single line representing some of the information about this process.
     */
    @Override
    public String toString() {
        // No bloody string formatting in Java 1.4. Starting to reconsider support for it.
        // Even C can do this ffs
        return stringFormat(pid, 5) + " " + stringFormat(name, 10) + " " + stringFormat(parentPid, 5) + " "
                + stringFormat(owner, 10) + " " + stringFormat(command, 23) + " " + stringFormat(nFDs, 8) + " "
                + stringFormat(nTasks, 8) + " " + cwd;
    }

    private static String stringFormat(int intToFormat, int fieldSize) {
        return stringFormat(Integer.toString(intToFormat), fieldSize, true);
    }

    private static String stringFormat(String stringToFormat, int fieldSize) {
        return stringFormat(stringToFormat, fieldSize, false);
    }

    private static String stringFormat(String stringToFormat, int fieldSize, boolean rightJustify) {
        // and Java doesn't really excel at this kind of thing either
        if (stringToFormat.length() >= fieldSize) {
            return stringToFormat.substring(0, fieldSize);
        } else {
            return rightJustify ? PADDING.substring(0, fieldSize - stringToFormat.length()) + stringToFormat
                    : stringToFormat + PADDING.substring(0, fieldSize - stringToFormat.length());
        }
    }

    // gotta love this hack
    final private static String PADDING = "                                                                                   ";
}
