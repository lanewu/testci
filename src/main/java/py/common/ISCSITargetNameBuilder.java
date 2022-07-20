package py.common;

import org.apache.commons.lang3.Validate;

/**
 * 
 * @author zjm
 *
 */
public class ISCSITargetNameBuilder {
    private static final String TARGET_PREFIX = "Zettastor.IQN";
    private static final String SECOND_IQN_NAME_SPLIT_SIGN = "_";
    private static final String SECOND_IQN_NAME_POSTFIX = SECOND_IQN_NAME_SPLIT_SIGN + "1";

    public static String build(long volumeId,int snapshotId) {
        return String.format("%s:%s-%s", TARGET_PREFIX, volumeId,snapshotId);
    }

    public static String buildSecondIqnName(long volumeId,int snapshotId) {
        return build(volumeId,snapshotId) + SECOND_IQN_NAME_POSTFIX;
    }

    public static String buildSecondIqnName(String firstIqnName) {
        return firstIqnName + SECOND_IQN_NAME_POSTFIX;
    }

    public static long parseVolumeId(String targetName) {
        String originVolumeName = targetName.split(":")[1];
        Validate.notEmpty(originVolumeName, "can not be null %s", targetName);
        String returnString = null;
        if (originVolumeName.contains(SECOND_IQN_NAME_POSTFIX)) {
            returnString = originVolumeName.split(SECOND_IQN_NAME_SPLIT_SIGN)[0];
            Validate.notEmpty(returnString, "can not be null %s", originVolumeName);
        } else {
            returnString = originVolumeName;
        }
        return Long.parseLong(returnString);
    }
}
