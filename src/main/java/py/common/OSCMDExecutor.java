package py.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author zjm
 *
 */
public class OSCMDExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(OSCMDExecutor.class);

    public static interface OSCMDStreamConsumer {
        public void consume(InputStream stream) throws IOException;
    }

    /**
     * The instance of this class just consume all output in kind STDOUT and STDERR.
     * 
     * @author zjm
     *
     */
    public static class OSCMDNullConsumer implements OSCMDStreamConsumer {

        @Override
        public void consume(InputStream stream) throws IOException {
            while (stream.read() >= 0) {

            }
        }
    }

    /**
     * The instance of this class logging each line of output in kind STDOUT and STDERR of some OS command after
     * running.
     * 
     * @author zjm
     *
     */
    public static class OSCMDOutputLogger implements OSCMDStreamConsumer {
        private final Logger logger;

        private final String osCMD;

        private boolean errorStream = false;

        public OSCMDOutputLogger(String osCMD) {
            this(LOG, osCMD);
        }

        public OSCMDOutputLogger(Logger logger, String osCMD) {
            this.logger = logger;
            this.osCMD = osCMD;
        }

        public boolean isErrorStream() {
            return errorStream;
        }

        public void setErrorStream(boolean errorStream) {
            this.errorStream = errorStream;
        }

        @Override
        public void consume(InputStream stream) throws IOException {
            String line = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                if (isErrorStream()) {
                    logger.warn("An output line for command [ {} ]: {}", osCMD, line);
                } else {
                    logger.info("An output line for command [ {} ]: {}", osCMD, line);
                }
            }
        }
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * 
     * @param osCMD
     * @param streamConsumeExecutor
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, ExecutorService streamConsumeExecutor,
            OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        return exec(osCMD, null, streamConsumeExecutor, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * 
     * @param osCMD
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param streamConsumeExecutor
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp, ExecutorService streamConsumeExecutor,
            OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        return exec(osCMD, envp, null, streamConsumeExecutor, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * 
     * @param osCMD
     * @param streamConsumeExecutor
     *            thread pool for STDOUT consumer and STDERR consumer.
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp, File dir, ExecutorService streamConsumeExecutor,
            OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        Process osCMDProcess;
        InputStream stdoutStream, stderrStream;
        Future<?> stdoutConsumerFuture, stderrConsumerFuture;

        osCMDProcess = Runtime.getRuntime().exec(osCMD, envp, dir);
        stdoutStream = osCMDProcess.getInputStream();
        stderrStream = osCMDProcess.getErrorStream();

        stdoutConsumerFuture = streamConsumeExecutor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    stdoutStreamConsumer.consume(stdoutStream);
                } catch (IOException e) {
                    LOG.error("Caught an exception when executing os command [ {} ]", osCMD, e);
                }
            }
        });

        stderrConsumerFuture = streamConsumeExecutor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    stdoutStreamConsumer.consume(stderrStream);
                } catch (IOException e) {
                    LOG.error("Caught an exception when executing os command [ {} ]", osCMD, e);
                }
            }
        });

        osCMDProcess.waitFor();
        try {
            stdoutConsumerFuture.get();
            stderrConsumerFuture.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Caught an exception", e);
            throw new IOException(e);
        }

        return osCMDProcess.exitValue();
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     *
     * @param osCMDs
     * @param streamConsumeExecutor
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, ExecutorService streamConsumeExecutor,
                           OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        return exec(osCMDs, null, streamConsumeExecutor, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     *
     * @param osCMDs
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param streamConsumeExecutor
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, ExecutorService streamConsumeExecutor,
                           OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        return exec(osCMDs, envp, null, streamConsumeExecutor, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     *
     * @param osCMDs
     * @param streamConsumeExecutor
     *            thread pool for STDOUT consumer and STDERR consumer.
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, File dir, ExecutorService streamConsumeExecutor,
                           OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        return exec(osCMDs, envp, dir, null, streamConsumeExecutor, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     *
     * @param osCMDs
     * @param promptValue
     *            specify string value which can be piped to process of the given command, e.g. yes/no...
     * @param streamConsumeExecutor
     *            thread pool for STDOUT consumer and STDERR consumer.
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, File dir, String promptValue,
                           ExecutorService streamConsumeExecutor, OSCMDStreamConsumer stdoutStreamConsumer,
                           OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        Process osCMDProcess;
        OutputStream outputStream;
        InputStream stdoutStream, stderrStream;
        Future<?> stdoutConsumerFuture, stderrConsumerFuture;

        osCMDProcess = Runtime.getRuntime().exec(osCMDs, envp, dir);
        stdoutStream = osCMDProcess.getInputStream();
        stderrStream = osCMDProcess.getErrorStream();

        stdoutConsumerFuture = streamConsumeExecutor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    stdoutStreamConsumer.consume(stdoutStream);
                } catch (IOException e) {
                    LOG.error("Caught an exception when executing os command [ {} ]", osCMDs, e);
                }
            }
        });

        stderrConsumerFuture = streamConsumeExecutor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    stderrStreamConsumer.consume(stderrStream);
                } catch (IOException e) {
                    LOG.error("Caught an exception when executing os command [ {} ]", osCMDs, e);
                }
            }
        });

        if (promptValue != null) {
            outputStream = osCMDProcess.getOutputStream();
            try {
                outputStream.write(promptValue.getBytes());
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOG.warn("Unable to close pipe to command: {}", osCMDs, e);
                }
            }
        }

        osCMDProcess.waitFor();
        try {
            stdoutConsumerFuture.get();
            stderrConsumerFuture.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Caught an exception", e);
            throw new IOException(e);
        }

        return osCMDProcess.exitValue();
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     * 
     * @param osCMD
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, OSCMDStreamConsumer stdoutStreamConsumer,
            OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMD, (String[]) null, (File) null, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, String[], ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather
     * than this method.
     * 
     * @param osCMD
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp, OSCMDStreamConsumer stdoutStreamConsumer,
            OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMD, envp, (File) null, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     * 
     * @param osCMD
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp, File dir, OSCMDStreamConsumer stdoutStreamConsumer,
            OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        int exitCode;
        ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "OS CMD Consumer");
            }
        });

        exitCode = exec(osCMD, envp, dir, executorService, stdoutStreamConsumer, stderrStreamConsumer);
        executorService.shutdown();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            LOG.warn("OS command consumers for [ {} ] havn't been finished yet!", osCMD);
        }
        return exitCode;
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, OSCMDStreamConsumer stdoutStreamConsumer,
                           OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMDs, (String[]) null, (File) null, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param promptValue
     *            specify string value which can be piped to process of the given command, e.g. yes/no...
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String promptValue, OSCMDStreamConsumer stdoutStreamConsumer,
                           OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMDs, promptValue, (String[]) null, (File) null, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, String[], ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather
     * than this method.
     *
     * @param osCMDs
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, OSCMDStreamConsumer stdoutStreamConsumer,
                           OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMDs, envp, (File) null, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, File dir, OSCMDStreamConsumer stdoutStreamConsumer,
                           OSCMDStreamConsumer stderrStreamConsumer) throws IOException, InterruptedException {
        return exec(osCMDs, null, envp, dir, stdoutStreamConsumer, stderrStreamConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param promptValue
     *            specify string value which can be piped to process of the given command, e.g. yes/no...
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @param stdoutStreamConsumer
     * @param stderrStreamConsumer
     * @return exit code of OS command
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String promptValue, String[] envp, File dir,
                           OSCMDStreamConsumer stdoutStreamConsumer, OSCMDStreamConsumer stderrStreamConsumer)
            throws IOException, InterruptedException {
        int exitCode;
        ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "OS CMD Consumer");
            }
        });

        exitCode = exec(osCMDs, envp, dir, promptValue, executorService, stdoutStreamConsumer, stderrStreamConsumer);
        executorService.shutdown();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            LOG.warn("OS command consumers for [ {} ] havn't been finished yet!", osCMDs);
        }
        return exitCode;
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     * 
     * @param osCMD
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD) throws IOException, InterruptedException {
        return exec(osCMD, (String[]) null, (File) null);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     * 
     * @param osCMD
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp) throws IOException, InterruptedException {
        return exec(osCMD, envp, null);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     * 
     * @param osCMD
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String osCMD, String[] envp, File dir) throws IOException, InterruptedException {
        OSCMDNullConsumer osCMDConsumer = new OSCMDNullConsumer();
        return exec(osCMD, envp, dir, osCMDConsumer, osCMDConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs) throws IOException, InterruptedException {
        return exec(osCMDs, (String[]) null, (File) null);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param promptValue
     *            specify string value which can be piped to process of the given command, e.g. yes/no...
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String promptValue) throws IOException, InterruptedException {
        return exec(osCMDs, promptValue, (String[]) null, (File) null);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp) throws IOException, InterruptedException {
        return exec(osCMDs, envp, null);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String[] envp, File dir) throws IOException, InterruptedException {
        OSCMDNullConsumer osCMDConsumer = new OSCMDNullConsumer();
        return exec(osCMDs, envp, dir, osCMDConsumer, osCMDConsumer);
    }

    /**
     * Execute the given OS command with STDOUT consumer and STDERR consumer which are both instance of
     * {@link OSCMDNullConsumer}.
     * <p>
     * This method will create a fix size thread pool for consuming STDOUT and STDERR stream for each invoke. For
     * resource better management, it is prefer to use
     * {@link OSCMDExecutor#exec(String, ExecutorService, OSCMDStreamConsumer, OSCMDStreamConsumer)} rather than this
     * method.
     *
     * @param osCMDs
     * @param promptValue
     *            specify string value which can be piped to process of the given command, e.g. yes/no...
     * @param envp
     *            array of strings, each element of which has environment variable settings in the format name=value, or
     *            null if the subprocess should inherit the environment of the current process.
     * @param dir
     *            the working directory of the subprocess, or null if the subprocess should inherit the working
     *            directory of the current process.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(String[] osCMDs, String promptValue, String[] envp, File dir)
            throws IOException, InterruptedException {
        OSCMDNullConsumer osCMDConsumer = new OSCMDNullConsumer();
        return exec(osCMDs, promptValue, envp, dir, osCMDConsumer, osCMDConsumer);
    }
}
