package py.processmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.processmanager.exception.PMDBPathNotExist;

/**
 * db that backup service status, service process pid, pm process pid.
 * 
 * when shutdown a service, first a shutdown command was send. if this did not work, we should kill service by its pid.
 * 
 * when service was shutdown, the pm process should exit as normal. But if the process did not exit, we should kill pm
 * process by its pid.
 * 
 * so we should backup service status, service process pid, pm process pid.
 * 
 * In this case, we backup data in a local file.
 * 
 * @author liy
 * 
 */
public class PMDB {
    private static final Logger logger = LoggerFactory.getLogger(PMDB.class);

    public static final String SERVICE_STATUS_NAME = "Status";
    
    public static final String SERVICE_STATUS_NAME_BAK = "Status_bak";

    public static final String PM_PID_NAME = "PMPid";

    public static final String SERVICE_PID_NAME = "SPid";

    public static final String COORDINATOR_PIDS_DIR_NAME = String.format("%s_coordinator", SERVICE_PID_NAME);

    private Path dbPath;

    public static PMDB build(Path dbPath) throws PMDBPathNotExist {
        PMDB pmdb = new PMDB(dbPath);
        return pmdb;
    }

    public PMDB(Path dbPath) throws PMDBPathNotExist {
        if (!Files.exists(dbPath)) {
            logger.error("Unable to find path {}", dbPath);
            throw new PMDBPathNotExist();
        }

        this.dbPath = dbPath;
    }

    /**
     * Save records to specified file db.
     * 
     * @param fileDBName
     *            file name which stores all records
     * @param records
     *            all records to store in file db
     * @return a value of boolean type which tells if the action is successful.
     *         <p>
     *         true: successfully save to the file
     *         <p>
     *         false: failed to save to the file
     */
    public boolean save(String fileDBName, String records) {
        logger.debug("Save {} to file {} in {}", records, fileDBName, dbPath.toString());

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getFile(fileDBName)));
            writer.write(records);
            writer.flush();
        } catch (IOException e) {
            logger.error("Caught an exception when save records to file", e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Caught an exception when close writer of file {}", fileDBName, e);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get all records from specified file.
     * 
     * @param fileDBName
     *            where all records stored in
     * @return <p>
     *         not null: all records stored in the file
     *         <p>
     *         null: something wrong when get all records from the file
     */
    public String get(String fileDBName) {
        logger.debug("Get records from file {} in {}", fileDBName, dbPath.toString());

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getFile(fileDBName)));

            String line = reader.readLine();
            if (line != null) {
                return line;
            }
        } catch (FileNotFoundException e) {
            logger.error("No such file {} in path {}", fileDBName, dbPath.toString());
            return null;
        } catch (IOException e) {
            logger.error("Caught an io exception when get records from {}", fileDBName, e);
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    private File getFile(String fileDBName) {
        String fileDBPath = Paths.get(dbPath.toFile().getAbsolutePath(), fileDBName).toString();

        return new File(fileDBPath);
    }
}
