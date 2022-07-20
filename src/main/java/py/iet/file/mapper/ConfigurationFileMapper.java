package py.iet.file.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class read or write properties of iscsi enterprise targets from file. Only if IET restart, the file will be applied
 * by it to config targets.
 *
 * @author zjm
 */
public class ConfigurationFileMapper implements IETFileMapper {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileMapper.class);

    /**
     * A class assembles some of lun properties.
     *
     * @author zjm
     */
    public static class Lun {
        private int index;

        private String path;

        private String type;

        private long blockSize;

        private long blocks;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getBlocks() {
            return blocks;
        }

        public void setBlocks(long blocks) {
            this.blocks = blocks;
        }

        public long getBlockSize() {
            return blockSize;
        }

        public void setBlockSize(long blockSize) {
            this.blockSize = blockSize;
        }

        @Override
        public String toString() {
            return "Lun{" +
                    "index=" + index +
                    ", path='" + path + '\'' +
                    ", type='" + type + '\'' +
                    ", blockSize=" + blockSize +
                    ", blocks=" + blocks +
                    '}';
        }
    }


    /**
     * A class assembles properties of target.
     *
     * @author zjm
     */
    public static class Target {
        private String targetName;

        private List<Lun> lunList;

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public List<Lun> getLunList() {
            return lunList;
        }

        public void setLunList(List<Lun> lunList) {
            this.lunList = lunList;
        }

        public void addToLunList(Lun lun) {
            if (lunList == null) {
                lunList = new ArrayList<Lun>();
            }

            lunList.add(lun);
        }

        @Override
        public int hashCode() {
            return targetName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            Target other = (Target) obj;
            return this.targetName.equals(other.getTargetName());
        }

        @Override
        public String toString() {
            return "Target [targetName=" + targetName + ", lunList=" + lunList + "]";
        }
    }

    /*
     * default IET configuration file path
     */
    public String filePath;// = "/etc/iet/ietd.conf";

    private List<Target> targetList = new ArrayList<Target>();

    /**
     * A method to parse target properties and lun properties from specific file to memory. The method to parse target
     * is in rule which comes out by observation of the file.
     *
     * @return
     */
    @Override
    public boolean load() {
        if (targetList != null) {
            targetList.clear();
        }

        try {
            Target target = null;
            BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                // clear leading whitespace of configuration line
                readLine = readLine.trim();

                // ignore comments in configuration file
                Pattern commentPattern = Pattern.compile("#");
                Matcher commentMatcher = commentPattern.matcher(readLine);
                if (commentMatcher.find(0)) {
                    continue;
                }

                // parse target properties
                Pattern targetPattern = Pattern.compile("target\\s+", Pattern.CASE_INSENSITIVE);
                Matcher targetMatcher = targetPattern.matcher(readLine);
                if (targetMatcher.find(0)) {
                    target = new Target();
                    String targetName = readLine.split("\\s+")[1];
                    target.setTargetName(targetName);
                    addToTargetList(target);
                    continue;
                }

                // parse lun properties
                Pattern lunPattern = Pattern.compile("lun\\s+", Pattern.CASE_INSENSITIVE);
                Matcher lunMatcher = lunPattern.matcher(readLine);
                if (lunMatcher.find(0) && target != null) {
                    Lun lun = new Lun();
                    int lunIndex = Integer.parseInt(readLine.split("\\s+")[1]);
                    System.out.println(lunIndex);
                    lun.setIndex(lunIndex);
                    target.addToLunList(lun);

                    String propsString = readLine.split("\\s+")[2];
                    for (String prop : propsString.split(",")) {
                        prop = prop.toLowerCase();
                        if (prop.contains("path")) {
                            String path = prop.split("=")[1];
                            lun.setPath(path);
                        }

                        if (prop.contains("type")) {
                            String type = prop.split("=")[1];
                            lun.setType(type);
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.error("Caught an exception when load file {}", filePath);
            return false;
        }

        return true;
    }

    /**
     * A method to flush memory data structure to IET configuration file in its format.
     *
     * @return
     */
    @Override
    public boolean flush() {
        try {
            List<String> commentList = new ArrayList<String>();
            if (new File(filePath).exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
                String readLine = null;
                while ((readLine = reader.readLine()) != null) {
                    Pattern commentPattern = Pattern.compile("\\s*#");
                    Matcher commentMatcher = commentPattern.matcher(readLine);
                    if (commentMatcher.find(0)) {
                        commentList.add(readLine);
                    }
                }
                reader.close();
            }

            BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(filePath));
            for (String comment : commentList) {
                bufferWriter.write(comment);
                bufferWriter.newLine();
            }

            if (targetList != null && targetList.size() > 0) {
                for (Target target : targetList) {
                    String targetLine = String.format("Target %s", target.getTargetName());
                    bufferWriter.write(targetLine);
                    bufferWriter.newLine();

                    if (target.getLunList() == null || target.getLunList().size() == 0) {
                        continue;
                    }

                    for (Lun lun : target.getLunList()) {
                        String lunLine = String.format("\tLun %d path=%s,type=%s", lun.getIndex(), lun.getPath(),
                                lun.getType());
                        bufferWriter.write(lunLine);
                        bufferWriter.newLine();
                    }
                }
            }

            bufferWriter.flush();
            bufferWriter.close();
        } catch (Exception e) {
            logger.error("Caught an exception when flush targets {} to file {}", targetList, filePath, e);
            return false;
        }

        return true;
    }

    public void addToTargetList(Target target) {
        if (targetList == null) {
            targetList = new ArrayList<Target>();
        }

        targetList.add(target);
    }

    public List<Target> getTargetList() {
        return targetList;
    }

    public void setTargetList(List<Target> targetList) {
        this.targetList = targetList;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
