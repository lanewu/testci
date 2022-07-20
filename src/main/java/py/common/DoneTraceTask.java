package py.common;

import com.lmax.disruptor.dsl.Disruptor;
import py.engine.DelayedTask;
import py.engine.Result;

/**
 * Created by zhongyuan on 17-7-10.
 */
public class DoneTraceTask extends DelayedTask {
    private Disruptor<LoggerEvent> disruptor;
    private LoggerEvent loggerEvent;

    public DoneTraceTask(int delayMs, Disruptor<LoggerEvent> disruptor, LoggerEvent loggerEvent) {
        super(delayMs);
        this.disruptor = disruptor;
        this.loggerEvent = loggerEvent;
    }

    @Override
    public Result work() {
        LoggerTracer.publishForDoneTrace(disruptor, loggerEvent);
        return null;
    }
}
