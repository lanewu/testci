package py.common.log.info.carrier;

/**
 * Created by zhongyuan on 17-8-8.
 */
public interface LogInfoCarrier {
    public String buildLogInfo();

    public void release();
}
