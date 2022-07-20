package py.common.struct;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by zhongyuan on 17-7-11.
 */
public class PYRingBuffer<T> {
    public final static String ELEMENT_IDENTIFIER = "element";

    private final int limit; // queue size

    private LongAdder nextWriteIndex;

    private LongAdder headIndex;

    private Object[] elementData;

    public PYRingBuffer(int limit) {
        this.limit = limit;
        this.elementData = new Object[this.limit];
        this.nextWriteIndex = new LongAdder();
        this.nextWriteIndex.reset();
        this.headIndex = new LongAdder();
        this.headIndex.reset();
    }

    public void offer(Object dataValue) {
        long writeIndex = nextWriteIndex.longValue();

        elementData[(int) (writeIndex % limit)] = dataValue;
        nextWriteIndex.increment();

        if ((writeIndex + 1) > limit) {
            headIndex.increment();
        }
    }

    /**
     * this call will move read index forward, please make sure you only want read once
     *
     * @return
     */
    public T next() {
        long readIndex = headIndex.longValue();
        if (readIndex >= nextWriteIndex.longValue()) {
            return null;
        }

        T t = (T) elementData[(int) (readIndex % limit)];
        headIndex.increment();
        return t;
    }

    /**
     * this call won't move read index
     *
     * @return
     */
    public T getFirst() {
        T t = (T) elementData[(int) (headIndex.longValue() % limit)];
        return t;
    }

    /**
     * this call won't move read index
     */
    public T getLast() {
        long gotNextWriteIndex = nextWriteIndex.longValue();
        if (gotNextWriteIndex == 0) {
            return null;
        }
        int readIndex = (int) ((gotNextWriteIndex - 1) % limit);
        T t = (T) elementData[readIndex];
        return t;
    }

    public int size() {
        long gotNextWriteIndex = nextWriteIndex.longValue();

        if (gotNextWriteIndex > limit) {
            return limit;
        } else {
            return (int) gotNextWriteIndex;
        }
    }

    public int getLimit() {
        return limit;
    }

    public boolean isFull() {
        return size() == limit;
    }

    /**
     * @return
     */
    public String buildString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PYRingBuffer:\n");
        while (true) {
            T element = next();
            if (element == null) {
                break;
            } else {
                stringBuilder.append(ELEMENT_IDENTIFIER);
                stringBuilder.append(":[");
                stringBuilder.append(element);
                stringBuilder.append("],\n");
            }
        }
        return stringBuilder.toString();
    }

    public void release() {
        this.elementData = null;
    }

}
