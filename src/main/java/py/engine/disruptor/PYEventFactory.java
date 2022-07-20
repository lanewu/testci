package py.engine.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * Created by zhongyuan on 17-6-8.
 */
public class PYEventFactory implements EventFactory<PYEvent> {
    @Override
    public PYEvent newInstance() {
        return new PYEventImpl();
    }
}
