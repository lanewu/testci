package py.common.log.info.carrier;

/**
 * Created by zhongyuan on 17-8-8.
 */
public class SimpleLogInfoCarrier implements LogInfoCarrier {
    private String msg;

    public SimpleLogInfoCarrier(String msg) {
        this.msg = msg;
    }

    @Override
    public String buildLogInfo() {
        return this.msg;
    }

    @Override
    public void release() {
        this.msg = null;
    }
}
