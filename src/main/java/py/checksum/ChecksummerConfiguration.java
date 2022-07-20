package py.checksum;

import py.exception.UnsupportedChecksumAlgorithmException;

public class ChecksummerConfiguration {
    /**
     * the algorithms of checksum are supported in my system, if you want to add new algorithm, you should add the name
     * of algorithm.
     * 
     * @author lx-lc-lx
     *
     */
    public enum Algorithm {
        DUMMY(0x4775FBD5),

        ALDER32(0x6C542c22),

        DIGEST(0x7F723B01),

        CRC32(0x569CF412),

        CRC32C(0x845BC521);

        private final int value;

        private Algorithm(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Algorithm findByMagicNum(int magicNum) throws UnsupportedChecksumAlgorithmException {
            switch (magicNum) {
            case 0x569CF412:
                return CRC32;
            case 0x4775FBD5:
                return DUMMY;
            case 0x6C542c22:
                return ALDER32;
            case 0x7F723B01:
                return DIGEST;
            case 0x845BC521:
                return CRC32C;
            default:
                throw new UnsupportedChecksumAlgorithmException("not support the magic number: " + magicNum);
            }
        }

        public int getChecksumMagicNum() {
            return value;
        }
    }

    /**
     * the digest checksum algorithm can be implemented by multiple method, you can set digest name as follow.
     * 
     * @author lx
     *
     */
    public enum DigestName {
        MD5, SHA
    }

    private String digestName = DigestName.MD5.name();
    private Algorithm algorithm;

    public ChecksummerConfiguration() {
        algorithm = Algorithm.CRC32;
    }

    public void setAlgorithm(Algorithm algorithm) {
        /**
         * configer alorithm by use cfg item.at last must set checksumMagicNum by algorithm in PageChecksumHelper
         */
        this.algorithm = algorithm;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public String getDigestName() {
        return digestName;
    }

    public void setDigestName(String digestName) {
        this.digestName = digestName;
    }
}