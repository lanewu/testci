package py.RandomSequential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IOStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(IOStateMachine.class);
    State state = State.IO_RANDOM;
    boolean offsetContinues ;
    AtomicInteger sequentialCount = new AtomicInteger(0);
    AtomicInteger randomCount = new AtomicInteger(0);
    AtomicLong lastOffset = new AtomicLong(0);

    Sequential sequentialFMS;
    Random randomFMS;

    public IOStateMachine(int sequentialConditionThreshold, int randomConditionThreshold) {
        sequentialFMS = new Sequential(randomConditionThreshold, sequentialConditionThreshold, sequentialCount, randomCount);
        randomFMS = new Random(randomConditionThreshold, sequentialConditionThreshold, sequentialCount, randomCount);
        logger.warn("io state machine sequential threshold:{} random threshold:{}", sequentialConditionThreshold, randomConditionThreshold);
    }

    public State process(long offset, int length) {
        offsetContinues = lastOffset.get() == offset;
        switch (state) {
        case IO_RANDOM:
            state = randomFMS.process(offsetContinues);
            break;
        case IO_SEQUENTIAL:
            state = sequentialFMS.process(offsetContinues);
            break;
        default:
            break;
        }
        lastOffset.set(offset + length);
        return state;
    }

    class Random extends FSM {
        public Random(int randomCondition, int sequentialCondition, AtomicInteger sequentialCount,
                AtomicInteger randomCount) {
            super(randomCondition, sequentialCondition, sequentialCount, randomCount);
        }

        State process(boolean offsetContinues) {
            if (offsetContinues) {
                if (sequentialCount.incrementAndGet() > sequentialCondition) {
                    randomCount.set(0);
                    return State.IO_SEQUENTIAL;
                }
            } else {
                randomCount.incrementAndGet();
                sequentialCount.set(0);
            }
            return State.IO_RANDOM;
        }
    }

    class Sequential extends FSM {
        public Sequential(int randomCondition, int sequentialConditon, AtomicInteger sequentialCount,
            AtomicInteger randomCount) {
            super(randomCondition, sequentialConditon, sequentialCount, randomCount);
        }

        State process(boolean offsetContinues) {
            if (offsetContinues) {
                if (sequentialCount.incrementAndGet() >= sequentialCondition) {
                    randomCount.set(0);
                }
            } else {
                sequentialCount.set(0);
                if (randomCount.incrementAndGet() > randomCondition) {
                    return State.IO_RANDOM;
                }
            }
            return State.IO_SEQUENTIAL;
        }
    }

    class FSM {
        protected int randomCondition;
        protected int sequentialCondition;
        protected AtomicInteger sequentialCount;
        protected AtomicInteger randomCount;

        public FSM(int randomCondition, int sequentialCondition, AtomicInteger sequentialCount,
                AtomicInteger randomCount) {
            this.randomCondition = randomCondition;
            this.sequentialCondition = sequentialCondition;
            this.sequentialCount = sequentialCount;
            this.randomCount = randomCount;
        }
    }

    enum State {
        IO_RANDOM, IO_SEQUENTIAL,
    }

}
