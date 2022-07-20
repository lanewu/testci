package py.netty.message;

/**
 * Created by zhongyuan on 18-8-21.
 */
public enum MethodType {
    PING(0), WRITE(1), READ(2), COPY(3), BACKWARDSYNCLOG(4), SYNCLOG(5), CHECK(
            6), GIVEYOULOGID(7), GETMEMBERSHIP(8), ADDORCOMMITLOGS(9), DISCARD(10), STARTONLINEMIGRATION(11), INVALID(-1);
    private final int value;

    private MethodType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
