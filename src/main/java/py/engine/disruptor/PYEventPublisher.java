package py.engine.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Created by zhongyuan on 17-6-8.
 */
public class PYEventPublisher {
    static class TranslatorOneArg<T> implements EventTranslatorOneArg<PYEvent, T> {
        @Override
        public void translateTo(PYEvent event, long sequence, T data) {
            event.setData(data);
        }
    }

    public static TranslatorOneArg TranslatorOneArg = new TranslatorOneArg();

    public static void publishEvent(PYDisruptor pyDisruptor, Long data) {
        Validate.notNull(pyDisruptor.getDisruptor());
        RingBuffer<PYEvent> ringBuffer = pyDisruptor.getDisruptor().getRingBuffer();
        ringBuffer.publishEvent(TranslatorOneArg, data);
    }
}
