package py.connection.pool;

import io.netty.channel.Channel;
import py.common.struct.EndPoint;

/**
 * The class is mainly for bind a new channel from netty and get {@link Channel} for checking connection.
 * <p>
 * Created by kobofare on 2017/3/4.
 */
class PYChannel {

    private volatile Channel channel;
    private long lastIoTime;
    private final EndPoint endPoint;

    /**
     * when channel call close() which is asynchronous, mark it as closing status to avoid next connect request trigger reconnecting progress.
     * {@link ConnectionRequest} can do reconnect only {@link PYChannel} is not closing status.
     */
    private boolean closing;

    public PYChannel(EndPoint endPoint) {
        this.lastIoTime = System.currentTimeMillis();
        this.channel = null;
        this.endPoint = endPoint;
    }

    public Channel get() {
        return channel;
    }

    public Channel set(Channel newChannel) {
        Channel oldChannel = channel;
        channel = newChannel;
        return oldChannel;
    }

    public long getLastIoTime() {
        return lastIoTime;
    }

    public void setLastIoTime(long lastIoTime) {
        this.lastIoTime = lastIoTime;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public boolean isClosing() {
        return closing;
    }

    public void setClosing(boolean closing) {
        this.closing = closing;
    }

    @Override
    public String toString() {
        return "PYChannel{" + "channel=" + channel + ", lastIoTime=" + lastIoTime + ", endPoint=" + endPoint
                + ", closing=" + closing + '}';
    }
}
