package py.common.struct;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;

import static py.common.Utils.LINE_SEPARATOR;

/**
 * Created by zhongyuan on 17-2-9.
 */

public class LimitQueue<T> {

    private final int limit; // queue size

    private LinkedBlockingDeque<T> queue = new LinkedBlockingDeque<>();

    public LimitQueue(int limit) {
        this.limit = limit;
    }

    public boolean offer(T t) {
        boolean dropHead = false;
        if (queue.size() >= limit) {
            queue.poll();
            dropHead = true;
        }
        queue.offer(t);
        return dropHead;
    }

    public T getLast() {
        T element = queue.getLast();
        return element;
    }

    public T getFirst() {
        T element = queue.getFirst();
        return element;
    }

    public int size() {
        return queue.size();
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LimitQueue:\n");
        synchronized (queue) {
            Iterator<T> iterator = queue.iterator();
            while (iterator.hasNext()) {
                T element = iterator.next();
                stringBuilder.append("element:[");
                stringBuilder.append(element);
                stringBuilder.append("],\n");
                // stringBuilder.offer(LINE_SEPARATOR);
            }
        }
        return stringBuilder.toString();
    }

    // add just for unit test
    public LinkedBlockingDeque<T> getQueue() {
        return queue;
    }

}
