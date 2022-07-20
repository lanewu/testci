package py.common;

import py.common.log.info.carrier.LogInfoCarrier;
import py.metrics.PYTimerContext;

/**
 * Created by zhongyuan on 17-6-4.
 */
public class LoggerEvent {

    private TraceType traceType;
    private long traceId;

    private LogInfoCarrier logInfoCarrier;
    private TraceAction traceAction;
    private String className;
    private int timeoutMs;
    private long endTime;

    private PYTimerContext fromPublishToHandle;

    public LoggerEvent() {
    }

    public void deepCopy(LoggerEvent other) {
        if (other != null) {
            if (other.getTraceType() != null) {
                this.traceType = other.getTraceType();
            }

            this.traceId = other.getTraceId();

            if (other.getLogInfoCarrier() != null) {
                this.logInfoCarrier = other.getLogInfoCarrier();
            }
            if (other.getTraceAction() != null) {
                this.traceAction = other.getTraceAction();
            }
            if (other.getClassName() != null) {
                this.className = other.getClassName();
            }
            this.timeoutMs = other.getTimeoutMs();
            this.endTime = other.getEndTime();
        }
    }

    public void setTraceType(TraceType traceType) {
        this.traceType = traceType;
    }

    public void setTraceId(long traceId) {
        this.traceId = traceId;
    }

    public void setTraceAction(TraceAction traceAction) {
        this.traceAction = traceAction;
    }

    public TraceType getTraceType() {
        return traceType;
    }

    public long getTraceId() {
        return traceId;
    }

    public TraceAction getTraceAction() {
        return traceAction;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isMark() {
        return traceType == TraceType.MARK;
    }

    public boolean isDoneTrace() {
        return traceType == TraceType.DONETRACE;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public LogInfoCarrier getLogInfoCarrier() {
        return logInfoCarrier;
    }

    public void setLogInfoCarrier(LogInfoCarrier logInfoCarrier) {
        this.logInfoCarrier = logInfoCarrier;
    }

    public void setFromPublishToHandle(PYTimerContext fromPublishToHandle) {
        this.fromPublishToHandle = fromPublishToHandle;
    }

    public void stopFromPublishToHandle() {
        if (fromPublishToHandle != null) {
            fromPublishToHandle.stop();
        }
    }

    public String formatLogger() {
        Long currentTimeMillis = System.currentTimeMillis();
        String timeToPrint = Utils.millsecondToString(currentTimeMillis);

        // format string
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(timeToPrint);
        stringBuilder.append("]");
        stringBuilder.append(" [");
        stringBuilder.append(className);
        stringBuilder.append("]");
        stringBuilder.append(logInfoCarrier.buildLogInfo());

        return stringBuilder.toString();
    }

    public void release() {
        if (logInfoCarrier != null) {
            logInfoCarrier.release();
            logInfoCarrier = null;
        }

        if (traceType != null) {
            traceType = null;
        }

        if (traceAction != null) {
            traceAction = null;
        }

        if (className != null) {
            className = null;
        }
    }

}
