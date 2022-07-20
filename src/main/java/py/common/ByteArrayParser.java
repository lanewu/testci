package py.common;

public interface ByteArrayParser<T> {
    T parse(byte[] byteArray);
}
