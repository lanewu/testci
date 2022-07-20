package py.storage;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by pengchen on 10/23/17.
 */
public class EdRootpathSingleton {
    private static final Logger logger = LoggerFactory.getLogger(EdRootpathSingleton.class);
    public static final String EVENT_DATA_PATH_PREFIX = "EventData";
    public static final String RECORD_POSITION_PATH_PREFIX = "RecordPosition";
    private volatile static EdRootpathSingleton instance;

    private String rootPath;

    private EdRootpathSingleton() {
    }

    public static EdRootpathSingleton getInstance() {
        if (instance == null) {
            synchronized (EdRootpathSingleton.class) {
                if (instance == null) {
                    instance = new EdRootpathSingleton();
                }
            }
        }
        return instance;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        logger.info("set event data root path:{}", rootPath);
        this.rootPath = rootPath;
    }

    public Path generateEventDataPath(String pyService) {
        Validate.notNull(getRootPath());
        Validate.notNull(pyService);
        logger.info("generate event data root path:{} with PyService:{}", getRootPath(), pyService);
        return Paths.get(getRootPath(), EVENT_DATA_PATH_PREFIX, pyService);
    }

    public Path generateRecordPath(PyService pyService) {
        Validate.notNull(getRootPath());
        Validate.notNull(pyService.getServiceName());
        logger.info("generate record data root path:{}", getRootPath());
        return Paths.get(getRootPath(), RECORD_POSITION_PATH_PREFIX, pyService.getServiceName());
    }

    public Path generateRecordPath(String applicationName) {
        Validate.notNull(applicationName);
        logger.info("generate record data root path:{}", getRootPath());
        return Paths.get(getRootPath(), RECORD_POSITION_PATH_PREFIX, applicationName);
    }
}
