package py.storage;

public class PYOSInfo {
    public enum OS {
        WINDOWS,
        LINUX,
        UNIX,
        MAC,
        OTHER;
        //TODO how get kylin
        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
    private static OS os = OS.OTHER;

    public static OS getOs() {
        String osName = System.getProperty("os.name");
        if(osName == null) {
            os = OS.OTHER;
        }
        osName = osName.toLowerCase();
        if (osName.contains("windows")) {
            os = OS.WINDOWS;
        } else if (osName.contains("linux")) {
            os = OS.LINUX;
        } else if(osName.contains("mac os")) {
            os = OS.MAC;
        } else if (osName.contains("sun os")
                || osName.contains("sunos")
                || osName.contains("solaris")) {
            os = OS.UNIX;
        } else {
            os = OS.OTHER;
        }
        os.setVersion(System.getProperty("os.version"));
        return os;
    }
}
