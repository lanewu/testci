package py.connection.pool.udp.detection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;

public class UdpDetectClient {
  private static final Logger logger = LoggerFactory.getLogger(UdpDetectClient.class);

  private static final int UDP_DETECT_MAGIC = 0xABC312;
  private static final int RECEIVE_BUFFER_SIZE = 512;

  private final DatagramSocket client;
  private final byte[] receiveBuffer;

  public UdpDetectClient() throws SocketException {
    this.client = new DatagramSocket();
    this.receiveBuffer = new byte[RECEIVE_BUFFER_SIZE];
  }

  void receive(Consumer<EndPoint> consumer) throws IOException {

    DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

    while (true) {

      client.receive(packet);

      try {

        logger.debug("receive data {}", packet.getData());
        Request request = Request.deserializeRequest(packet.getData());

        logger.debug("receive request {}", request);

        consumer.accept(request.endPoint);

      } catch (DeserializeFailedException e) {
        logger.warn("failed to deserialize request", e);
      }

    }
  }

  void send(EndPoint endPoint, long requestId) throws IOException {

    Request request = new Request(requestId, endPoint);

    logger.debug("sending request {}", request);

    byte[] sendData = request.serializeRequest();

    Validate.isTrue(sendData.length < receiveBuffer.length);

    SocketAddress address = new InetSocketAddress(endPoint.getHostName(), endPoint.getPort());
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address);

    client.send(sendPacket);

    logger.debug("send data {}", sendPacket.getData());
  }

  private static class Request {
    private long requestId;
    private EndPoint endPoint;

    static Request deserializeRequest(byte[] data) throws DeserializeFailedException {

      try {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int magic = buffer.getInt();
        if (magic != UDP_DETECT_MAGIC) {
          logger.error("wrong magic {}, expected {}", magic, UDP_DETECT_MAGIC);
          throw new DeserializeFailedException();
        }

        long requestId = buffer.getLong(); // ignored

        int dataLength = buffer.getInt();

        byte[] ep = new byte[dataLength];
        buffer.get(ep);

        return new Request(requestId, new EndPoint(new String(ep)));

      } catch (DeserializeFailedException e) {
        throw e;
      } catch (Throwable t) {
        throw new DeserializeFailedException(t);
      }
    }

    Request(long requestId, EndPoint endPoint) {
      this.requestId = requestId;
      this.endPoint = endPoint;
    }

    byte[] serializeRequest() {

      byte[] ep = endPoint.toString().getBytes();
      byte[] head = head(requestId, ep.length);

      return ArrayUtils.addAll(head, ep);

    }

    private static byte[] head(long requestId, int dataLength) {
      ByteBuffer head = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES + Long.BYTES);
      head.putInt(UDP_DETECT_MAGIC);
      head.putLong(requestId);
      head.putInt(dataLength);
      return head.array();
    }

    @Override
    public String toString() {
      return "Request{" +
          "requestId=" + requestId +
          ", endPoint=" + endPoint +
          '}';
    }
  }

  private static class DeserializeFailedException extends Exception {

    private DeserializeFailedException(Throwable t) {
      super(t);
    }

    private DeserializeFailedException() {
    }

  }

}
