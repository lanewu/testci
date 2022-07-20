package py.common;

import com.lmax.disruptor.EventFactory;

/**
 * Created by zhongyuan on 17-6-4.
 */
public class LoggerEventFactory implements EventFactory<LoggerEvent> {
    @Override
    public LoggerEvent newInstance() {
        return new LoggerEvent();
    }
}
