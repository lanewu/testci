package py.common;

/**
 * Created by zhongyuan on 17-5-19.
 */
public enum TraceAction {
    Write(1), Read(2), CommitLog(3), CleanTraceLog(4);

    TraceAction(int value) {
        this.value = value;
    }

    private int value;
}
