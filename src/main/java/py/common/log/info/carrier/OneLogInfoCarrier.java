package py.common.log.info.carrier;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import py.common.LoggerTracer;

/**
 * Created by zhongyuan on 17-8-8.
 */
public class OneLogInfoCarrier implements LogInfoCarrier {
    private String format;
    private Object object;

    public OneLogInfoCarrier(String format, Object object) {
        this.format = format;
        this.object = object;
    }

    @Override
    public String buildLogInfo() {
        FormattingTuple ft = MessageFormatter.format(format, object);
        return LoggerTracer.buildString(ft);
    }

    @Override
    public void release() {
        this.format = null;
        this.object = null;
    }
}
