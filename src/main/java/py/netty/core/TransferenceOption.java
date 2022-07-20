package py.netty.core;

import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;

/**
 * Option items used for I/O transference.
 * 
 * @author zjm
 *
 * @param <T>
 */
public class TransferenceOption<T> {
    public static final ConcurrentMap<String, Boolean> names = PlatformDependent.newConcurrentHashMap();

    /*
     * Name of the option.
     */
    private String name;

    public static final TransferenceOption<Integer> MAX_MESSAGE_LENGTH = valueOf("MAX_MESSAGE_LENGTH");
    public static final TransferenceOption<Integer> MAX_BYTES_ONCE_ALLOCATE = valueOf(
        "MAX_BYTES_ONCE_ALLOCATE");

    private static <T> TransferenceOption<T> valueOf(String name) {
        return new TransferenceOption<T>(name);
    }

    protected TransferenceOption(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        if (names.putIfAbsent(name, Boolean.TRUE) != null) {
            throw new IllegalArgumentException(String.format("'%s' is already in use", name));
        }

        this.name = name;
    }

    public String name() {
        return this.name;
    }
}
