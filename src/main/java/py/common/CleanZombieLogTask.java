package py.common;

import py.engine.DelayedTask;
import py.engine.Result;

/**
 * Created by zhongyuan on 17-5-17.
 */
public class CleanZombieLogTask extends DelayedTask {

    private final Long traceLogId;

    public CleanZombieLogTask(int delayMs, long traceLogId) {
        super(delayMs);
        this.traceLogId = traceLogId;
    }

    @Override
    public Result work() {
        LoggerTracer.getInstance().doneTrace(this.traceLogId, TraceAction.CleanTraceLog, 0);
        // for now do not care about the result of done trace
        return null;
    }
}
