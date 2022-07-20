package py.engine.disruptor;

/**
 * Created by zhongyuan on 17-6-8.
 */
public class PYEventImpl<T> implements PYEvent<T> {
    private T data;
    @Override
    public void setData(T data) {
        this.data = data;
    }

    @Override
    public T getData() {
        return data;
    }
}
