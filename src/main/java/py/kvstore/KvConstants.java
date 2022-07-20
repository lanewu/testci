package py.kvstore;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class KvConstants {
    public static final long NO_LEASE = 0;

    public static byte[] int2bytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(value);
        return buffer.array();
    }

    public static Integer bytes2int(byte[] bytes) {
        return bytes == null ? null : ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] long2bytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        return buffer.array();
    }

    public static Long bytes2long(byte[] bytes) {
        return bytes == null ? null : ByteBuffer.wrap(bytes).getLong();
    }

    public static String bytes2str(byte[] bytes) {
        return bytes == null ? null : new String(bytes, UTF_8);
    }
}
