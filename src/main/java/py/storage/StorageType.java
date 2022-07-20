package py.storage;

public enum StorageType {
    SATA(0), SSD(1), PCIE(2);

    private final int value;

    private StorageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StorageType findByValue(int value) {
        switch (value) {
        case 0:
            return SATA;
        case 1:
            return SSD;
        case 2:
            return PCIE;
        default:
            return null;
        }
    }
}
