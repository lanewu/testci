package py.monitor.jmx.configuration;

public enum Switcher {
    ON("on"), OFF("off");

    private String value;

    private Switcher(String value) {
        this.value = value.toLowerCase();
    }

    public String value() {
        return value;
    }
}