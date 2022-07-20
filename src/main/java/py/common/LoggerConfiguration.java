package py.common;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class LoggerConfiguration {
    private final static String CONSOLE_APPENDER_NAME = "pengyun_console";
    private final static String FILE_APPENDER_NAME = "pengyun_file";
    private final static PatternLayout layout = new PatternLayout("%-5p[%d][%t]%C(%L):%m%n");
    private static LoggerConfiguration loggerConfiguration = null;
    private static Object sync = new Object();

    public static LoggerConfiguration getInstance() {
        if (loggerConfiguration == null) {
            synchronized (sync) {
                if (loggerConfiguration == null) {
                    loggerConfiguration = new LoggerConfiguration();
                }
            }
        }
        return loggerConfiguration;
    }

    private LoggerConfiguration() {
        // configures the root logger
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);

        // configures the root logger
        rootLogger.removeAllAppenders();

        Logger logger1 = Logger.getLogger("org.hibernate");
        logger1.setLevel(Level.WARN);

        Logger logger2 = Logger.getLogger("org.springframework");
        logger2.setLevel(Level.WARN);

        Logger logger3 = Logger.getLogger("com.opensymphony");
        logger3.setLevel(Level.WARN);

        Logger logger4 = Logger.getLogger("org.apache");
        logger4.setLevel(Level.WARN);

        Logger logger5 = Logger.getLogger("com.googlecode");
        logger5.setLevel(Level.WARN);

        Logger logger6 = Logger.getLogger("com.twitter.common.stats");
        logger6.setLevel(Level.WARN);

        Logger logger7 = Logger.getLogger("com.mchange");
        logger7.setLevel(Level.WARN);
    }

    public void appendFile(String fileName, Level level) {
        RollingFileAppender rollingFileAppender = new RollingFileAppender();
        rollingFileAppender.setName(FILE_APPENDER_NAME);
        rollingFileAppender.setFile(fileName);
        rollingFileAppender.setLayout(layout);
        rollingFileAppender.setThreshold(level);
        rollingFileAppender.setMaxBackupIndex(2);
        rollingFileAppender.setMaxFileSize("100MB");
        rollingFileAppender.activateOptions();
        Logger.getRootLogger().addAppender(rollingFileAppender);

    }

    public void appendConsole(Level level) {
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setName(CONSOLE_APPENDER_NAME);
        consoleAppender.setLayout(layout);
        consoleAppender.setThreshold(level);
        consoleAppender.setTarget("System.out");
        consoleAppender.setEncoding("UTF-8");
        consoleAppender.activateOptions();
        Logger.getRootLogger().addAppender(consoleAppender);
    }

    public Level getConsoleLevel() {
        return (Level) ((ConsoleAppender) (Logger.getRootLogger().getAppender(CONSOLE_APPENDER_NAME))).getThreshold();
    }

    public void setConsoleLevel(Level level) {
        ((ConsoleAppender) (Logger.getRootLogger().getAppender(CONSOLE_APPENDER_NAME))).setThreshold(level);
    }

    public Level getFileLevel() {
        return (Level) ((RollingFileAppender) (Logger.getRootLogger().getAppender(FILE_APPENDER_NAME))).getThreshold();
    }

    public void setFileLevel(Level level) {
        ((RollingFileAppender) (Logger.getRootLogger().getAppender(FILE_APPENDER_NAME))).setThreshold(level);
    }
}
