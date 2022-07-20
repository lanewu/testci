package py.common.lock;

public interface HashLock<T> {

    void lock(T val) throws InterruptedException;

    void unlock(T val);

}
