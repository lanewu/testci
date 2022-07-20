package py.connection.pool.udp;

public interface UDPServer {
  void startEchoServer() throws Exception ;

  void stopEchoServer();
}
