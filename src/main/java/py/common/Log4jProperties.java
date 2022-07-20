package py.common;

import java.util.Properties;

import org.apache.log4j.Level;

/**
 * Configuration for log4j in common use.
 * 
 * @author zjm
 *
 */
public class Log4jProperties {
    /*
     * Decide whether log4j configuration is persistent by situation in which
     * our system is used. Always persist in release version, not persist in
     * internal test.
     */
    private static final boolean PERSISTENT = false;

    private static final Level PERSISTENT_LEVEL = Level.WARN;

    private static Properties log4jProperties = new Properties();

    /**
     * Return log4j properties according to name of output log file and level of
     * log.
     * 
     * @param logFileName
     * @param logLevel
     * @return
     */
    public static Properties getProperties(String logOutputFile, Level logLevel) {
        /*
         * Refuse any modification on log level if log4j configuration is
         * persistent.
         */
        if (PERSISTENT) {
            log4jProperties.put("log4j.rootLogger", PERSISTENT_LEVEL + ", STDOUT, FILE");
        } else {
            log4jProperties.put("log4j.rootLogger", logLevel + ", STDOUT, FILE");
        }

        log4jProperties.put("log4j.appender.FILE", "org.apache.log4j.RollingFileAppender");
        log4jProperties.put("log4j.appender.FILE.File", logOutputFile);
        log4jProperties.put("log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout");
        log4jProperties.put("log4j.appender.FILE.layout.ConversionPattern", "%-5p[%d][%t]%C(%L):%m%n");
        log4jProperties.put("log4j.appender.FILE.MaxBackupIndex", "10");
        log4jProperties.put("log4j.appender.FILE.MaxFileSize", "400MB");

        log4jProperties.put("log4j.appender.STDOUT", "org.apache.log4j.ConsoleAppender");
        log4jProperties.put("log4j.appender.STDOUT.layout", "org.apache.log4j.PatternLayout");
        log4jProperties.put("log4j.appender.STDOUT.layout.ConversionPattern", "%-5p[%d][%t]%C(%L):%m%n");

        log4jProperties.put("log4j.logger.org.hibernate", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.org.springframework", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.com.opensymphony", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.org.apache", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.com.googlecode", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.com.twitter.common.stats", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.com.mchange", "ERROR, STDOUT, FILE");
        log4jProperties.put("log4j.logger.py.datanode.DataNodeAppEngine", "DEBUG, STDOUT, FILE");

        return log4jProperties;
    }
}
