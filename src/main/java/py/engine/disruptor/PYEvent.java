package py.engine.disruptor;

/**
 * Created by zhongyuan on 17-6-8.
 */
public interface PYEvent<T> {
    public void setData(T data);
    public T getData();
}
