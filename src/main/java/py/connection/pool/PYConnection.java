package py.connection.pool;

import py.netty.message.Message;
import py.netty.message.SendMessage;

/**
 * The class is used for sending requests through network.
 *
 * @author lx
 *
 */
public interface PYConnection {

    public boolean isConnected();

    public void write(Message msg);

    public void writeAndFlush(Message msg);

    public void flush();
}
