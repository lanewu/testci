package py.kvstore.etcd;

import io.etcd.jetcd.ByteSequence;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Constants {
    public static long NO_LEASE_ID = 0;

    public static ByteSequence fromStr(String string) {
        return ByteSequence.from(string, UTF_8);
    }

    public static ByteSequence fromBytes(byte[] bytes) {
        return ByteSequence.from(bytes);
    }

    public static String toStr(ByteSequence bs) {
        return bs.toString(UTF_8);
    }

    public static byte[] toBytes(ByteSequence bs) {
        return bs.getBytes();
    }
}
