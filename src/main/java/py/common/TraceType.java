package py.common;

/**
 * Created by zhongyuan on 17-6-4.
 */
public enum TraceType {
    MARK(1), DONETRACE(2);
    private int value;

    TraceType(int value) {
        this.value = value;
    }
}
