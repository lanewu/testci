package py.checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.checksum.ChecksummerConfiguration.Algorithm;
import py.exception.UnsupportedChecksumAlgorithmException;

/**
 * it will produce one checksummer from cfg or specified Algorithm to calculate checksum according the algorithm which is configured in configuration
 * file.
 * 
 * @author lx-lc
 * 
 */
public class NetworkChecksummerFactoryHelper {
    private static final Logger logger = LoggerFactory.getLogger(NetworkChecksummerFactoryHelper.class);

    public final static ChecksummerConfiguration cfg = new ChecksummerConfiguration();
    private final static ChecksummerFactory factory = new ChecksummerFactory(cfg);

    public static Checksummer create() {
        Checksummer checksummer = null;
        try {
            checksummer = factory.create();
        } catch (UnsupportedChecksumAlgorithmException e) {
            /**
             * it is a fatal error, so we should shutdown the application
             */
            logger.error("caught an exception", e);
            System.exit(0);
        }
        return checksummer;
    }
    
    public static Checksummer create(Algorithm algorithm) {
        Checksummer checksummer = null;
        try {
            checksummer = factory.create(algorithm);
        } catch (UnsupportedChecksumAlgorithmException e) {
            /**
             * it is a fatal error, so we should shutdown the application
             */
            logger.error("caught an exception", e);
            System.exit(0);
        }
        return checksummer;
    }
}