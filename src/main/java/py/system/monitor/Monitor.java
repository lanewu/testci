package py.system.monitor;

public interface Monitor {
    /**
     * Get the operating system name.
     *
     * @return The operating system name.
     */
    public String osName();

    /**
     * Gets the pid of the process that is calling this method (assuming it is running in the same process).
     *
     * @return The pid of the process calling this method.
     */
    public int currentPid();

    /**
     * Get process information of the given PID.
     * 
     * @param pid
     * @return instance of {@link ProcessInfo} or null if no such process.
     */
    public ProcessInfo processInfo(int pid);

    /**
     * Get the current process table. This call returns an array of objects, each of which represents a single process.
     *
     * @return An array of objects, each of which represents a process.
     */
    public ProcessInfo[] processTable();

    /**
     * Attempts to kill the process identified by the integer id supplied. This will silently fail if you don't have the
     * authority to kill that process. This method sends SIGTERM on the UNIX platform.
     *
     * @param pid
     *            The id of the process to kill
     */
    public void killProcess(int pid);
}
