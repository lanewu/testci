package py.common;

/**
 * function object.
 * 
 * @author shixulu
 *
 * @param <ArgType>
 *            type of the argument
 */
public interface Functor<ArgType> {
    void invoke(ArgType arg) throws Exception;
}
