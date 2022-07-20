package py.connection.pool.udp.detection;

import java.util.concurrent.CompletableFuture;
import py.common.struct.EndPoint;

public interface UdpDetector {

  CompletableFuture<Boolean> detect(String ip);

  CompletableFuture<Boolean> detect(EndPoint endPoint);

}
