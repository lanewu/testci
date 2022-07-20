package py.common.log.info.carrier;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import py.common.LoggerTracer;

/**
 * Created by zhongyuan on 17-8-8.
 */
public class TwoLogInfoCarrier implements LogInfoCarrier {
    private String format;
    private Object object1;
    private Object object2;

    public TwoLogInfoCarrier(String format, Object object1, Object object2) {
        this.format = format;
        this.object1 = object1;
        this.object2 = object2;
    }

    @Override
    public String buildLogInfo() {
        FormattingTuple ft = MessageFormatter.format(format, object1, object2);
        return LoggerTracer.buildString(ft);
    }

    @Override
    public void release() {
        this.format = null;
        this.object1 = null;
        this.object2 = null;
    }

}
