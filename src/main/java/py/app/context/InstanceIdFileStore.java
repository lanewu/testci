package py.app.context;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.instance.InstanceId;

/**
 * This class is a file that stores instanceId in its string format. 
 * 
 * For instance, an instance id is 132, then "132" is stored to the file. 
 * 
 * This class is not thread safe, and usually the class is only used when a service starts.
 * 
 * 
 * @author chenlia
 *
 */
public class InstanceIdFileStore implements InstanceIdStore {
    private static final Logger logger = LoggerFactory.getLogger(InstanceIdFileStore.class);

    private final static Charset ENCODING = StandardCharsets.UTF_8;
    private InstanceId instanceId;
    private Path pathToFile;

    @Override
    public InstanceId getInstanceId() {
        return instanceId;
    }

    // Get an instance id from instance name and the service port that the instance is listening to
    public InstanceIdFileStore(String root, String instanceName, int port) {

        // The system should have a file that persists instance id for this service
        pathToFile = constructFileName(root, instanceName, port);
        try {
            // First, create the directory where the instance file is stored
            if (!Files.exists(pathToFile.getParent())) {
                Files.createDirectories(pathToFile.getParent());
            }

            if (Files.exists(pathToFile, LinkOption.NOFOLLOW_LINKS)) {
                // The file exists, read it and parse the instanceId
                List<String> lines = Files.readAllLines(pathToFile, ENCODING);
                if (lines.isEmpty()) {
                    // Generate a new instanceId and persist it to the file
                    instanceId = new InstanceId();
                    // serialize to a string
                    syncWrite(instanceId.toRawString());
                    logger.warn("instance store file exists but empty, should generate a new instance id:{}",
                            instanceId);
                } else {
                    Validate.isTrue(lines.size() == 1);
                    // deseriazlize from its string form
                    instanceId = new InstanceId(lines.get(0));
                    logger.warn("instance store file exists and use the existing instance id: {}, file Path: {}",
                            instanceId, pathToFile.toAbsolutePath());
                }
            } else {
                Files.createFile(pathToFile);
                // Generate a new instanceId and persist it to the file
                instanceId = new InstanceId();
                syncWrite(instanceId.toRawString());
                logger.warn("instance store file does not exist and generate a new instance id:{}", instanceId);
            }
        } catch (Exception e) {
            String errMsg = "Can't get the instance id from file:" + pathToFile;
            throw new RuntimeException(errMsg, e);
        }
    }

    @Override
    public void persistInstanceId(InstanceId id) {
        // The system have already have a file that persists instance id for this service
        try {
            syncWrite(instanceId.toRawString());
            logger.warn("persisted instance id: {}, pathToFile: {}", instanceId, pathToFile);
            instanceId = id;
        } catch (Exception e) {
            String errMsg = "Can't persist the instance id";
            throw new RuntimeException(errMsg, e);
        }
    }

    private Path constructFileName(String root, String instanceName, int port) {
        return Paths.get(root, instanceName + "_" + port + "_instanceId");
    }

    // Persist the id in the its string form to a file
    // This function opens the file every time and close it after the persistence
    private void syncWrite(String str) {
        PrintStream printStream = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pathToFile.toFile());
            printStream = new PrintStream(fileOutputStream);
            printStream.print(str);
            printStream.flush();
            fileOutputStream.getFD().sync();
        } catch (Exception e) {
            logger.error("failed sync writing data:{} to file:{}", str, pathToFile.toString(), e);
        } finally {
            if (printStream != null) {
                try {
                    printStream.close();
                } catch (Exception e1) {
                    logger.error("can't close file output stream", e1);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "InstanceIdFileStore [instanceId=" + instanceId + ", pathToFile=" + pathToFile + "]";
    }

}
