package py.connection.pool.udp;

/**
 * Created by kobofare on 2017/8/18.
 */
public class UDPEchoServer {
    static {
        System.loadLibrary("udpServer");
    }

    public native int startEchoServer(int port);

    public native int stopEchoServer(int socketId);

    public native void pauseEchoServer();

    public native void reviveEchoServer();
}
