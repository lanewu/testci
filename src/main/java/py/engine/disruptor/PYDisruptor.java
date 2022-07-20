package py.engine.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * Created by zhongyuan on 17-6-8.
 */
public class PYDisruptor {
    private static final Logger logger = LoggerFactory.getLogger(PYDisruptor.class);
    private static final int ringBufferSize = 1024 * 1024;

    private final Disruptor<PYEvent> disruptor;
    private int numProcessor;

    public PYDisruptor(ProducerType producerType, int numProcessor, WaitStrategy waitStrategy) {
        PYEventFactory pyEventFactory = new PYEventFactory();
        this.disruptor = new Disruptor(pyEventFactory, ringBufferSize, Executors.defaultThreadFactory(), producerType,
                waitStrategy);
        this.numProcessor = numProcessor;
    }

    public void start() {
        this.disruptor.start();
    }

    public void shutdown() {
        this.disruptor.shutdown();
    }

    /**
     * user pass event handlers prepared.
     */
    public void initEventHandlers(EventHandler<PYEvent>[] eventHandlers) {
        if (eventHandlers.length != numProcessor) {
            logger.error("event handler number:{} not equals to number:{} target before, check it!",
                    eventHandlers.length, numProcessor);
            Validate.isTrue(false);
        }
        this.disruptor.handleEventsWith(eventHandlers);
    }

    /**
     * user pass event handlers prepared.
     */
    public void initSingleEventHandler(EventHandler<PYEvent> eventHandler) {
        if (numProcessor != 1) {
            logger.error("event handler number:1 not equals to number:{} target before, check it!",
                    numProcessor);
            Validate.isTrue(false);
        }
        this.disruptor.handleEventsWith(eventHandler);
    }

    /**
     * get for publish
     * @return
     */
    public Disruptor<PYEvent> getDisruptor() {
        return disruptor;
    }
}
