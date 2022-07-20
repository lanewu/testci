package py.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static final byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();

    public static long convertSecondToNanosecond(int second){
        return TimeUnit.SECONDS.toNanos(second);
    }


    public static int convertNanosecondToSecond(long nanosecond){
        return (int) TimeUnit.NANOSECONDS.toSeconds(nanosecond);
    }

    public static int convertNanosecondToMilliSecond(long nanosecond){
        return (int)TimeUnit.NANOSECONDS.toMillis(nanosecond);
    }

    public static long convertMillisecondToNanosecond (int millisecond){
        return  (long)millisecond * 1000000;
    }

    public static String getPageIndex(String volumeId, int segId) {
        return volumeId == null ? null : volumeId + segId;
    }

    public static StringBuilder toString(ByteBuffer bb, StringBuilder sb, int numberToOutput) {
        int limit = (bb.remaining() > numberToOutput) ? numberToOutput : bb.remaining();

        for (int i = 0; i < limit; i++) {
            sb.append(" ");
            sb.append(paddedByteString(bb.get(i + bb.position())));
        }

        if (bb.remaining() > limit) {
            sb.append("...");
        }

        return sb;
    }

    public static String toString(byte[] data) {
        ByteBuffer bf = ByteBuffer.wrap(data);
        StringBuilder sb = new StringBuilder();
        toString(bf, sb);
        return sb.toString();
    }

    public static void toString(ByteBuffer bb, StringBuilder sb) {
        toString(bb, sb, 128);
    }

    public static String paddedByteString(byte b) {
        int extended = (b | 0x100) & 0x1ff;
        return Integer.toHexString(extended).toUpperCase().substring(1);
    }

    public static boolean isLocalIpAddress(String addr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            logger.warn("can't get inet address from " + addr, e);
            return false;
        }

        // Check if the address is a valid special local or loop back
        if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
            return true;
        }
        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(inetAddress) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public static boolean isIpAddress(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }

        String regex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(regex);

        Matcher mat = pat.matcher(addr);

        return mat.find();
    }

    public static final int getProcessID() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.valueOf(runtimeMXBean.getName().split("@")[0]).intValue();
    }

    public static final int getOpenFileAmountByPid() {
        int pid = getProcessID();
        int fileHandlerAmount = 0;
        try {
            String procPath = String.format("/proc/%d/fd", pid);
            logger.warn("proc path is:{}", procPath);
            Path path = Paths.get(procPath);
            if (Files.exists(path)) {
                File[] fileArray = path.toFile().listFiles();
                if (fileArray == null || fileArray.length == 0) {
                    return 0;
                }
                fileHandlerAmount = fileArray.length;
            }
        } catch (Exception e) {
            logger.error("caught an exception", e);
        }
        return fileHandlerAmount;
    }

    public static int getExponentialBackoffSleepTime(int sleepTimeUnit, int failureTimes, int maxBackoffTime) {
        int exponentialBackoff = sleepTimeUnit * (2 << failureTimes);
        if (exponentialBackoff <= 0 || exponentialBackoff > maxBackoffTime) {
            // overflowed or too large
            return maxBackoffTime;
        } else {
            return exponentialBackoff;
        }
    }

    // return real size of swapping file, maybe it exceeded the totalSize when we decreased the swapping page size and
    // restart the datanode service, so we can cacaluted the memory page size
    static public long createOrExtendFile(File file, long totalSize) throws IOException {
        long existingLength = 0;
        if (file.exists()) {
            existingLength = file.length();
        }

        long remaining = totalSize - existingLength;
        long fileSize = existingLength;

        // Let's use a block to write to a file instead of 1 byte each time
        // the block can be anything. use 128 * 1024;
        int blockSize = 128 * 1024;

        byte[] buffer = new byte[blockSize];

        // even if the file is large enough, lets open it -- make sure we have access
        // true signifies open for append.
        FileOutputStream fos = new FileOutputStream(file, true);
        try {
            while (remaining > 0) {
                int sizeToWrite = buffer.length < remaining ? buffer.length : (int) remaining;
                fos.write(buffer, 0, sizeToWrite);
                remaining -= sizeToWrite;
                fileSize += sizeToWrite;
            }
        } catch (IOException e) {
            logger.warn("can't not extend file: " + file.getAbsolutePath() + ", current file size: " + fileSize);
        } finally {
            fos.close();
        }

        return fileSize;
    }

    /**
     * @param milliseconds : the milliseconds need to change to string
     * @return the String of time can be readable by people;
     */
    public static String millsecondToString(long milliseconds) {
        // SimpleDateFormat is not thread safe, so we create separate format instances for each thread.
        SimpleDateFormat dateFormat = new SimpleDateFormat(getFormatType());
        Date date = new Date();
        date.setTime(milliseconds);
        return dateFormat.format(date);
    }

    public static String getFormatType() {
        return "yyyy-MM-dd HH:mm:ss:SSS";
    }

    public static long stringToMillsecond(String time) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(getFormatType());
        return formatter.parse(time).getTime();
    }

    public static void createFile(File file, long fileSize) throws IOException {
        if (file == null) {
            logger.error("Invalid pram, file is null");
            throw new IOException();
        }
        String fileName = file.getAbsolutePath();
        RandomAccessFile raf = null;
        try {
            if (!file.exists()) {
                logger.warn("try to create file:{}, file length:{}", fileName, fileSize);
                raf = new RandomAccessFile(fileName, "rw");
                raf.setLength(fileSize);
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    /**
     * Delete file directly or delete directory recursively.
     *
     * @param file file to be deleted
     * @throws IOException if it is unable to delete some file or directory. Any file or directory processed by this method
     *                     before this exception will be deleted and those after this exception will still exist there. In
     *                     total, it is possible remaining files or directories.
     */
    public static void deleteFileOrDirectory(File file) throws IOException {
        LinkedList<File> heap;

        heap = new LinkedList<File>();
        heap.addFirst(file);
        while (heap.size() > 0) {
            boolean isSymbolic;

            file = heap.removeFirst();
            isSymbolic = Files.isSymbolicLink(Paths.get(file.getPath()));

            if ((isSymbolic || file.isFile()) && !file.delete()) {
                throw new IOException("Unable to delete file " + file);
            }

            if (file.isDirectory()) {
                File[] subEntries = file.listFiles();

                if (subEntries.length == 0 && !file.delete()) {
                    throw new IOException("Unable to delete directory " + file);
                }

                if (subEntries.length > 0) {
                    heap.addFirst(file);
                    for (File subEntry : subEntries) {
                        heap.addFirst(subEntry);
                    }
                }
            }
        }
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static void deleteEveryThingExceptDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return;
    }

    public static void mkDir(File file) {
        if (file.getParentFile().exists()) {
            file.mkdir();
        } else {
            mkDir(file.getParentFile());
            file.mkdir();
        }
    }

    public static <T> boolean compareTwoList(List<T> list1, List<T> list2) {
        if (list1 != null) {
            if (list2 == null) {
                return false;
            }
        } else {
            if (list2 != null) {
                return false;
            } else {
                return true;
            }
        }
        for (T obj1 : list1) {
            boolean found = false;
            for (T obj2 : list2) {
                if (obj1.equals(obj2)) {
                    found = true;
                }
            }
            // found obj1 in list2 in this loop
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * generate a value which belongs to [minValue, maxValue]
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    public static int generateRandomNum(int minValue, int maxValue) {
        Random random = new Random();
        return random.nextInt(maxValue) % (maxValue - minValue + 1) + minValue;
    }

    /**
     * Check if port is not being used by some process and available for a socket server.
     *
     * @param port
     * @return true if port is available for a socket server, or false.
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);

            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);

            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }

        return false;
    }

    /*
     * use it to check file can be read or write.
     */
    public boolean checkFileUsingCat(String filePath, int timeoutMS) {
        boolean checkSessionFileGood = true;
        Process p = null;
        try {
            String catCommand = "cat " + filePath;
            p = Runtime.getRuntime().exec(catCommand);
            checkSessionFileGood = p.waitFor(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            logger.error("file can not be read", e);
            return false;
        }
        return checkSessionFileGood;
    }

    /**
     * @deprecated use {@link OSCMDExecutor} instead
     */
    @Deprecated
    public interface CommandProcessor {
        //        public boolean processor(String line);
        public List<String> getNormalStream(String line);

        public List<String> getErrorStream(String line);

    }

    /**
     * This method executes the given command, and captures its exit code, normal output lines, and error output lines.
     * <p>
     *
     * @param commands    operation system command, it is necessary to make sure there is the command in system.
     * @return exit code after executing the given command
     * @throws Exception if something wrong when starting a process to execute the given command
     * @deprecated use {@link OSCMDExecutor} instead
     */
    @Deprecated

    public static int executeCommand(String commands, CommandProcessor processorCon) throws Exception {
        logger.debug("Executing command: {}", commands);

        try {
            Process process = Runtime.getRuntime().exec(commands);
            // process.waitFor();

            String line = null;
            BufferedReader normReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = normReader.readLine()) != null) {
                logger.debug("Command normal message output: \"{}\"", line);
                processorCon.getNormalStream(line);
            }
            StringBuilder errorMessageBuilder = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorMessageBuilder.append(line);
                errorMessageBuilder.append("\n");
                processorCon.getErrorStream(line);
            }

            process.waitFor();
            if (process.exitValue() != 0) {
                logger.error("Something wrong when execute command {}: \"{}\"", commands,
                        errorMessageBuilder.toString());
                return process.exitValue();
            }
        } catch (Exception e) {
            logger.error("Caught an exception when execute command {}", commands, e);
            throw e;
        }

        return 0;
    }

    /**
     * This method executes the given command, and captures its exit code, normal output lines, and error output lines.
     *
     * @param commands    operation system command, it is necessary to make sure there is the command in system.
     * @param outputLines normal output lines after executing the given command
     * @param errorLines  error output lines after executing the given command
     * @return exit code after executing the given command
     * @throws Exception if something wrong when starting a process to execute the given command
     * @deprecated use {@link OSCMDExecutor} instead
     */
    @Deprecated
    public static int executeCommand(String commands, List<String> outputLines, List<String> errorLines)
            throws Exception {
        logger.debug("Executing command: {}", commands);

        try {
            Process process = Runtime.getRuntime().exec(commands);
//            process.waitFor();

            String line = null;
            StringBuilder normMessageBuilder = new StringBuilder();
            BufferedReader normReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = normReader.readLine()) != null) {
                normMessageBuilder.append(line);
                normMessageBuilder.append("\n");

                if (outputLines != null) {
                    outputLines.add(line);
                }
            }


            StringBuilder errorMessageBuilder = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorMessageBuilder.append(line);
                errorMessageBuilder.append("\n");

                if (errorLines != null) {
                    errorLines.add(line);
                }
            }

            process.waitFor();


            logger.debug("Command output: \"{}\"", normMessageBuilder.toString());
            if (process.exitValue() != 0) {

                logger.error("Something wrong when execute command {}: \"{}\"", commands,
                        errorMessageBuilder.toString());
                return process.exitValue();
            }
        } catch (Exception e) {
            logger.error("Caught an exception when execute command {}", commands, e);
            throw e;
        }

        return 0;
    }

    public static int appearCount(String srcString, String findString) {
        int count = 0;
        Pattern p = Pattern.compile(findString);
        Matcher m = p.matcher(srcString);
        while (m.find()) {
            count++;
        }
        return count;
    }

    public static <T> T random(Collection<T> collection) {
        int select = ThreadLocalRandom.current().nextInt(collection.size());
        for (T t : collection) if (--select < 0) return t;
        throw new IllegalArgumentException("empty collection");
    }

    public static void waitUntilConditionMatches(int timeOutSeconds, BooleanSupplier condition)
            throws TimeoutException {
        waitUntilConditionMatches(timeOutSeconds, "condition", condition);
    }

    public static void waitUntilConditionMatches(int timeOutSeconds, BooleanSupplier condition, int checkIntervalMils)
            throws TimeoutException {
        waitUntilConditionMatches(timeOutSeconds, "condition", condition, VoidCallback.EMPTY_VOID_CALL_BACK,
                VoidCallback.EMPTY_VOID_CALL_BACK, checkIntervalMils);
    }

    public static void waitUntilConditionMatches(int timeOutSeconds, String conditionName, BooleanSupplier condition)
            throws TimeoutException {
        waitUntilConditionMatches(timeOutSeconds, conditionName, condition, VoidCallback.EMPTY_VOID_CALL_BACK,
                VoidCallback.EMPTY_VOID_CALL_BACK, 1000);
    }

    public static void waitUntilConditionMatches(int timeOutSeconds, String conditionName, BooleanSupplier condition,
            VoidCallback onSingleFailed, VoidCallback onFailed) throws TimeoutException {
        waitUntilConditionMatches(timeOutSeconds, conditionName, condition, onSingleFailed, onFailed, 1000);
    }

    public static void waitUntilConditionMatches(int timeOutSeconds, String conditionName, BooleanSupplier condition,
            VoidCallback onSingleFailed, VoidCallback onFailed, int checkIntervalMills) throws TimeoutException {
        int tick = 0;
        long timeoutMills = TimeUnit.SECONDS.toMillis(timeOutSeconds);
        while (true) {
            if (condition.getAsBoolean()) {
                logger.info("wait for {} cost {} seconds, timeout {}", conditionName,
                        TimeUnit.MILLISECONDS.toSeconds(tick), timeOutSeconds);
                break;
            } else {
                tick += checkIntervalMills;
                if (tick > timeoutMills) {
                    try {
                        onFailed.call();
                    } catch (Exception e) {
                        logger.error("error calling on fail", e);
                    }
                    throw new TimeoutException();
                } else {
                    try {
                        onSingleFailed.call();
                        logger.info("{}/{} waiting for {}...", TimeUnit.MILLISECONDS.toSeconds(tick), timeOutSeconds,
                                conditionName);
                        Thread.sleep(checkIntervalMills);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    public static CompletableFuture<Object> anyExceptionOrAllDone(
        Collection<CompletableFuture<?>> futures, boolean cancelOnException) {

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);

        for (CompletableFuture<?> future : futures) {
            future.exceptionally(t -> {
                failedFuture.completeExceptionally(t);
                return null;
            });
            successFuture = CompletableFuture.allOf(future, successFuture);
        }

        if (cancelOnException) {
            failedFuture.exceptionally(t -> {
                futures.forEach(f -> f.cancel(true));
                return null;
            });
        }

        return CompletableFuture.anyOf(failedFuture, successFuture);
    }

    public static CompletableFuture<Object> runParallel(Collection<Runnable> targets,
        Executor executorService, boolean cancelOnException) {

        List<CompletableFuture<?>> subFutures = new ArrayList<>();

        for (Runnable t : targets) {
            subFutures.add(CompletableFuture.runAsync(t, executorService));
        }

        return anyExceptionOrAllDone(subFutures, cancelOnException);

    }

    public static <T> CompletableFuture<Object> consumeParallel(Collection<T> targets,
        Executor executorService,
        Consumer<T> consumer, boolean cancelOnException) {

        List<Runnable> runnables = new ArrayList<>();
        for (T target : targets) {
            runnables.add(() -> consumer.accept(target));
        }

        return runParallel(runnables, executorService, cancelOnException);
    }

    public static String dumpArrayToHex(ByteBuffer buffer, long beginFlagOffset) {
        if (buffer.remaining() <= 0) {
            return StringUtils.EMPTY;
        }

        ByteBuffer duplicate = buffer.duplicate();
        byte[] array = new byte[duplicate.remaining()];
        duplicate.get(array);

        return dumpArrayToHex(array, beginFlagOffset);
    }

    public static String dumpArrayToHex(byte[] buf, long beginFlagOffset) {
        if (ArrayUtils.isEmpty(buf)) {
            return StringUtils.EMPTY;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            HexDump.dump(buf, beginFlagOffset, baos, 0);
            return baos.toString();
        } catch (IOException e) {
            logger.error("dump byte array failed.", e);
            return StringUtils.EMPTY;
        }
    }

    public static byte[] integerToBytes(int integer) {
        return ByteBuffer.allocate(4).putInt(integer).array();
    }
}
