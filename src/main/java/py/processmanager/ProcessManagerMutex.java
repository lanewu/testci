package py.processmanager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author yxr
 *
 */
public class ProcessManagerMutex {

    private static final Logger logger = LoggerFactory.getLogger(ProcessManagerMutex.class);
    static File file;
    static FileChannel fileChannel;
    static FileLock lock;
    private static String curDir = null;

    // check whether the process is processing
    @SuppressWarnings("resource")
    public static boolean checkIfAlreadyRunning(String dir) throws IOException {
        curDir = dir;
        // processmanagermutex.lock is preventing one servie from multiple processmanagers
        file = new File(curDir, "processmanagermutex.lock");
        fileChannel = new RandomAccessFile(file, "rw").getChannel();
        lock = fileChannel.tryLock();

        if (lock == null) {
            fileChannel.close();
            return false;
        }

        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        return true;
    }

    public static void unlockFile() throws IOException {

        if (lock != null) {
            lock.release();
            fileChannel.close();
        }
    }

    static class ShutdownHook extends Thread {
        public void run() {
            try {
                unlockFile();
            } catch (IOException e) {
                logger.error("Caught an exception when release lock of mutext process file", e);
            }
        }
    }
}
