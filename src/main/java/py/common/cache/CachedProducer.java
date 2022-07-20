package py.common.cache;

public interface CachedProducer<T> {
    T poll() throws Exception;
}
