package py.common.log.info.carrier;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import py.common.LoggerTracer;

/**
 * Created by zhongyuan on 17-8-8.
 */
public class MultiLogInfoCarrier implements LogInfoCarrier {
    private String format;
    private Object[] objects;

    public MultiLogInfoCarrier(String format, Object[] objects) {
        this.format = format;
        this.objects = objects;
    }

    @Override
    public String buildLogInfo() {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, objects);
        return LoggerTracer.buildString(ft);
    }

    @Override
    public void release() {
        this.format = null;
        this.objects = null;
    }
}
