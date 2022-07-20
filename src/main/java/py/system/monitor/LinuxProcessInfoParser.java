package py.system.monitor;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LinuxProcessInfoParser {
    private final String stat;
    private final String status;
    private final String cmdline;
    private final HashMap uids;
    private final String cwd;
    private final int nFDs;
    private final int nTasks;

    private static final Pattern STATUS_NAME_MATCHER = Pattern.compile("Name:\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern STATUS_UID_MATCHER = Pattern.compile("Uid:\\s+(\\d+)\\s.*", Pattern.MULTILINE);

    public LinuxProcessInfoParser(String stat, String status, String cmdline, HashMap uids, String cwd, int nFDs,
            int nTasks) {
        this.stat = stat;
        this.status = status;
        this.cmdline = cmdline;
        this.uids = uids;
        this.cwd = cwd;
        this.nFDs = nFDs;
        this.nTasks = nTasks;
    }

    public ProcessInfo parse() throws ParseException {
        int openParen = stat.indexOf("(");
        int closeParen = stat.lastIndexOf(")");
        if (openParen <= 1 || closeParen < 0 || closeParen > stat.length() - 2) {
            throw new ParseException("Stat '" + stat + "' does not include expected parens around process name");
        }

        // Start splitting after close of proc name
        String[] statElements = stat.substring(closeParen + 2).split(" ");
        if (statElements.length < 13) {
            throw new ParseException("Stat '" + stat + "' contains fewer elements than expected");
        }

        String pidStr = stat.substring(0, openParen - 1);

        int pid;
        int parentPid;
        try {
            pid = Integer.parseInt(pidStr);
            parentPid = Integer.parseInt(statElements[1]);
        } catch (NumberFormatException e) {
            throw new ParseException("Unable to parse stat '" + stat + "'");
        }

        return new ProcessInfo(pid, parentPid, trim(cmdline), getFirstMatch(STATUS_NAME_MATCHER, status),
                (String) uids.get(getFirstMatch(STATUS_UID_MATCHER, status)), cwd, nFDs, nTasks);
    }

    private String trim(String cmdline) {
        return cmdline.replace('\000', ' ').replace('\n', ' ');
    }

    public String getFirstMatch(Pattern pattern, String string) {
        try {
            Matcher matcher = pattern.matcher(string);
            matcher.find();
            return matcher.group(1);
        } catch (Exception e) {
            return "0";
        }
    }
}
