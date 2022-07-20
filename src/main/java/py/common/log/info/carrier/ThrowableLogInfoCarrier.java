package py.common.log.info.carrier;

import py.common.LoggerTracer;

/**
 * Created by zhongyuan on 17-8-8.
 */
public class ThrowableLogInfoCarrier implements LogInfoCarrier {
    private String msg;
    private Throwable t;

    public ThrowableLogInfoCarrier(String msg, Throwable t) {
        this.msg = msg;
        this.t = t;
    }

    @Override
    public String buildLogInfo() {
        return LoggerTracer.buildString(msg, t);
    }

    @Override
    public void release() {
        this.msg = null;
        this.t = null;
    }
}
