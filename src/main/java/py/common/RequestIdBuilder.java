package py.common;

import java.util.UUID;

public class RequestIdBuilder {
    public static long get() {
        long id = UUID.randomUUID().getLeastSignificantBits();
        if (id < 0) {
            id = id + Long.MAX_VALUE;
        }

        return id;
    }
}
