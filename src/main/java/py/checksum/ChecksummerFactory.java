package py.checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.checksum.ChecksummerConfiguration.Algorithm;
import py.exception.UnsupportedChecksumAlgorithmException;

/**
 * create a checksummer according the specific configuration.
 *
 * We can use ThreadLocal to optimize the speed of generating a checksummer. 
 *
 * something like:
 *
 *     private static ThreadLocal<MessageDigest> md5ThreadLocal = new ThreadLocal<MessageDigest>() {
 *        protected MessageDigest initialValue() {
 *         try {
 *              return MessageDigest.getInstance("MD5");
 *         } catch (NoSuchAlgorithmException e) {
 *            logger.error("error");
 *            return null;
 *         }
 *        };
 *    };
 * 
 * @author lx-lc
 * 
 */
public class ChecksummerFactory {
    private final static Logger logger = LoggerFactory.getLogger(ChecksummerFactory.class);

    private final ChecksummerConfiguration cfg;

    public ChecksummerFactory(ChecksummerConfiguration cfg) {
        this.cfg = cfg;
    }
    
    /**
     * create a checksummer by config file if there is no exception, or it will throw an UnsupportedChecksumAlgorithmException
     * 
     * @return
     * @throws UnsupportedChecksumAlgorithmException
     */
    public Checksummer create() throws UnsupportedChecksumAlgorithmException {
        try {
            return this.createCheckSummer(cfg.getAlgorithm());
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new UnsupportedChecksumAlgorithmException(e);
        }
    }
    
    /**
     * create a checksummer by specific algorithm if there is no exception, or it will throw an UnsupportedChecksumAlgorithmException
     * 
     * @return
     * @throws UnsupportedChecksumAlgorithmException
     */
    public Checksummer create(Algorithm algorithm) throws UnsupportedChecksumAlgorithmException {
        try {
            return this.createCheckSummer(algorithm);
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new UnsupportedChecksumAlgorithmException(e);
        }
    }
    
    private Checksummer createCheckSummer(Algorithm algorithm) throws UnsupportedChecksumAlgorithmException {
        try {
            if (algorithm == ChecksummerConfiguration.Algorithm.ALDER32) {
                return new Adler32Checksummer();
            } else if (algorithm == ChecksummerConfiguration.Algorithm.DIGEST) {
                return new DigestChecksummer(cfg.getDigestName());
            } else if (algorithm == ChecksummerConfiguration.Algorithm.DUMMY) {
                return new DummyChecksummer();
            } else if (algorithm == ChecksummerConfiguration.Algorithm.CRC32) {
                return new CRC32Checksummer();
            } else if (algorithm == ChecksummerConfiguration.Algorithm.CRC32C) {
                return new Crc32cChecksummer();
            } else {
                throw new UnsupportedChecksumAlgorithmException("not support algorithm: " + cfg.getAlgorithm());
            }
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new UnsupportedChecksumAlgorithmException(e);
        }
    }
}
