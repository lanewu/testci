package py.checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.UnsupportedChecksumAlgorithmException;

public class PageChecksummerConfiguration extends ChecksummerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PageChecksummerConfiguration.class);
    public void setAlgorithm(Algorithm algorithm) {
        /**
         * configer alorithm by use cfg item.at last must set checksumMagicNum
         * by algorithm in PageChecksumHelper,if not supported use default crc32
         */   
        try {
            PageChecksumHelper.setChecksumMagicNum(algorithm);
        } catch (UnsupportedChecksumAlgorithmException e) {
            logger.error("checksum config,not supported! Use default crc32!");
            throw new RuntimeException("not support the excetpion");
        }

        super.setAlgorithm(algorithm);
    }
}
