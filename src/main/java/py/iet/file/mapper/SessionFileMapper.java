package py.iet.file.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to read session properties of IET from specific file. As long as initiator has connected to the target,
 * properties of the session will be displayed in the file.
 * 
 * @author zjm
 *
 */
public class SessionFileMapper implements IETFileMapper {
    private static final Logger logger = LoggerFactory.getLogger(SessionFileMapper.class);

    public static class Initiator {
        private int cid;

        private long sid;

        private String ip;

        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public long getSid() {
            return sid;
        }

        public void setSid(long sid) {
            this.sid = sid;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

    }

    public static class Session {
        private int tid;

        private String targetName;

        private List<Initiator> initiatorList;

        public int getTid() {
            return tid;
        }

        public void setTid(int tid) {
            this.tid = tid;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public List<Initiator> getInitiatorList() {
            return initiatorList;
        }

        public void setInitiatorList(List<Initiator> initiatorList) {
            this.initiatorList = initiatorList;
        }

        public void addToInitiatorList(Initiator initiator) {
            if (initiatorList == null) {
                initiatorList = new ArrayList<Initiator>();
            }

            initiatorList.add(initiator);
        }

    }

    private String filePath;// = "/proc/net/iet/session";  //tid:2 name:Zettastor.IQN:724411276551444599    tid:1 name:Zettastor.IQN:2759199727108742608
    

    private List<Session> sessionList = new ArrayList<Session>();

    @Override
    public boolean load() {
        if (sessionList != null) {
            sessionList.clear();
        }

        // try use command: "cat /proc/net/iet/session" judge session file can be read or not

        try {
            Session session = null;
            Initiator initiator = null;
            BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                // clear leading whitespace of the line
                readLine = readLine.trim();

                // parse session properties from the file
                Pattern targetParttern = Pattern.compile("tid:[0-9]+");
                Matcher targetMatcher = targetParttern.matcher(readLine);
                if (targetMatcher.find(0)) {
                    session = new Session();
                    addToSessionList(session);
                    for (String sessionProp : readLine.split("\\s+")) {
                        if (sessionProp.contains("tid")) {
                            int tid = Integer.parseInt(sessionProp.split(":")[1]);
                            session.setTid(tid);
                        }

                        if (sessionProp.contains("name")) {
                            String targetName = sessionProp.substring(sessionProp.indexOf(":") + 1);
                            session.setTargetName(targetName);
                        }
                    }

                    continue;
                }

                Pattern sessionParttern = Pattern.compile("sid:[0-9]+");
                Matcher sessionMatcher = sessionParttern.matcher(readLine);
                if (sessionMatcher.find(0)) {
                    initiator = new Initiator();
                    session.addToInitiatorList(initiator);
                    for (String sessionProp : readLine.split("\\s+")) {
                        if (sessionProp.contains("sid")) {
                            long sid = Long.parseLong(sessionProp.split(":")[1]);
                            initiator.setSid(sid);
                        }
                    }
                }

                // parse initiator properties from the file
                Pattern initiatorPattern = Pattern.compile("cid:[0-9]+");
                Matcher initiatorMatcher = initiatorPattern.matcher(readLine);
                if (initiatorMatcher.find(0)) {
                    for (String initiatorProp : readLine.split("\\s+")) {
                        if (initiatorProp.contains("cid")) {
                            int cid = Integer.parseInt(initiatorProp.split(":")[1]);
                            initiator.setCid(cid);
                        }

                        if (initiatorProp.contains("ip")) {
                            int indexOfFirstColon;
                            InetAddress inetAddr;
                            String ip;
                            
                            /*
                             * Client info is recorded in session file and the line of client info looks like following:
                             * 
                             * "cid:0 ip:192.168.122.17 state:active hd:none dd:none"
                             * 
                             * "cid:0 ip:[fe80:0000:0000:0000:5054:00ff:fe54:4d0c] state:active hd:none dd:none"
                             * 
                             * The client address is followed with keyword "ip" and separated by colon ":".
                             */
                            indexOfFirstColon = initiatorProp.indexOf(":");
                            ip = initiatorProp.substring(indexOfFirstColon + 1);
                            inetAddr = InetAddress.getByName(ip); // remove "[]" enclosure ipv6 address.
                            initiator.setIp(inetAddr.getHostAddress());
                        }
                    }
                    continue;
                }
            }
        } catch (Exception e) {
            logger.error("Caught an exception when load file {}", filePath);
            return false;
        }

        return true;
    }

    public void addToSessionList(Session session) {
        if (sessionList == null) {
            sessionList = new ArrayList<Session>();
        }

        sessionList.add(session);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Session> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

    @Override
    public boolean flush() {
        throw new NotImplementedException(String.format("%s hasn't implement the interface", getClass().getName()));
    }
}
