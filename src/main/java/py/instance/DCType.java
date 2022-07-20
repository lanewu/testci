package py.instance;

public enum DCType {
    NORMAL_SUPPORT(0),  SCSI_SUPPORT(1), ALL_SUPPORT(2);

    private int value;

    DCType (int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DCType{" +
                "value=" + value +
                '}';
    }
}
