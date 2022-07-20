package py.storage.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import py.storage.Storage;

public class StorageUtils {
    public final static Pattern rawPattern = Pattern.compile("raw\\d+", Pattern.CASE_INSENSITIVE);
    public final static Pattern ssdPattern = Pattern.compile("ssd\\d+", Pattern.CASE_INSENSITIVE);
    public final static Pattern pciePattern = Pattern.compile("pcie\\d+", Pattern.CASE_INSENSITIVE);

    public static boolean isSATA(String devicePath) {
        String identifier = getDeviceName(devicePath);
        if (identifier != null) {
            Matcher matcher = rawPattern.matcher(identifier);
            return matcher.matches();
        }
        return false;
    }

    public static boolean isSATA(Storage storage) {
        return isSATA(storage.identifier());
    }

    public static boolean isSSD(String devicePath) {
        String identifier = getDeviceName(devicePath);
        if (identifier != null) {
            Matcher matcher = ssdPattern.matcher(identifier);
            return matcher.matches();
        }
        return false;
    }

    public static boolean isSSD(Storage storage) {
        return isSSD(storage.identifier());
    }

    public static boolean isPCIE(String devicePath) {
        String identifier = getDeviceName(devicePath);
        if (identifier != null) {
            Matcher matcher = pciePattern.matcher(identifier);
            return matcher.matches();
        }
        return false;
    }

    public static boolean isPCIE(Storage storage) {
        return isPCIE(storage.identifier());
    }

    private static String getDeviceName(String identifier) {
        String deviceName = identifier;
        if (deviceName != null && deviceName.contains("/")) {
            deviceName = deviceName.substring(deviceName.lastIndexOf('/') + 1);
        }

        return deviceName;
    }
}
