package py.netty.core;

/**
 * Created by fq on 19-5-21.
 */
public interface WriteInWTSLister<T> {
    void complete(T object);
}
