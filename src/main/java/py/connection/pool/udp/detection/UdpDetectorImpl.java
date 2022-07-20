package py.connection.pool.udp.detection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.util.HashedWheelTimer;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.NamedThreadFactory;
import py.common.struct.EndPoint;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

public enum UdpDetectorImpl implements UdpDetector {

  INSTANCE;

  private static final Logger logger = LoggerFactory.getLogger(UdpDetectorImpl.class);

  private final AtomicLong requestIdGenerator = new AtomicLong(0);

  private final AtomicBoolean started = new AtomicBoolean(false);

  private HashedWheelTimer timer;

  private Map<EndPoint, EndPointTask> pendingTasks;

  private int defaultPort;

  private UdpDetectClient client;

  private DetectionTimeoutPolicyFactory timeoutPolicyFactory;

  public UdpDetectorImpl start(int defaultPort, DetectionTimeoutPolicyFactory timeoutPolicyFactory)
      throws SocketException {

    if (started.compareAndSet(false, true)) {

      this.defaultPort = defaultPort;
      this.timeoutPolicyFactory = timeoutPolicyFactory;
      this.pendingTasks = new ConcurrentHashMap<>();

      try {
        this.client = new UdpDetectClient();
      } catch (SocketException e) {
        started.set(false);
        throw e;
      }

      this.timer = new HashedWheelTimer(new NamedThreadFactory("udp-timeout-handler", true), 5,
          TimeUnit.MILLISECONDS);
      this.timer.start();

      Thread receiveThread = new Thread(this::receiving, "udp-receive");
      receiveThread.setDaemon(true);
      receiveThread.start();

    } else {
      logger.warn("already started !! let's see who is calling", new Exception());
    }

    return this;
  }

  @Override
  public CompletableFuture<Boolean> detect(String ip) {
    return detect(new EndPoint(ip, defaultPort));
  }

  @Override
  public CompletableFuture<Boolean> detect(EndPoint endPoint) {
    EndPointTask task = pendingTasks
        .computeIfAbsent(endPoint,
            ep -> new EndPointTask(endPoint, timeoutPolicyFactory.generate()));
    task.start();
    return task.future;
  }

  private void receiving() {
    while (true) {
      try {
        client.receive(endPoint -> {

          EndPointTask task = pendingTasks.get(endPoint);
          if (task != null) {
            task.complete(true);
          }

        });
      } catch (IOException e) {
        logger.error("caught an io exception", e);
      }
    }
  }

  private void send(EndPoint ep) throws IOException {
    client.send(ep, requestIdGenerator.getAndIncrement());
  }

  private static final Map<EndPoint, PYMetric> metrics = new ConcurrentHashMap<>();

  private static PYMetric getMetricTimer(EndPoint ep) {
    return metrics.computeIfAbsent(ep,
        e -> PYMetricRegistry.getMetricRegistry()
            .register(MetricRegistry
                    .name("UdpDetectorImpl", "timer_detecting", ep.getHostName(),
                        Integer.toString(ep.getPort())),
                Timer.class));
  }

  class EndPointTask {

    private final EndPoint ep;
    private final CompletableFuture<Boolean> future;
    private final DetectionTimeoutPolicy timeoutPolicy;
    private final AtomicBoolean started;
    private final PYTimerContext timerContext;

    EndPointTask(EndPoint endPoint,
        DetectionTimeoutPolicy timeoutPolicy) {
      this.ep = endPoint;
      this.timeoutPolicy = timeoutPolicy;
      this.future = new CompletableFuture<>();
      this.started = new AtomicBoolean(false);
      this.timerContext = getMetricTimer(ep).time();
    }

    public void start() {
      if (started.compareAndSet(false, true)) {
        detect();
      }
    }

    public void detect() {

      if (future.isDone()) {
        return;
      }

      long timeoutMS = timeoutPolicy.getTimeoutMS();
      if (timeoutMS < 0) {
        complete(false);
        return;
      }

      try {
        send(ep);
      } catch (IOException e) {
        // sending failed, just ignore it
        logger.error("fail to send", e);
      }

      timer.newTimeout(t -> detect(), timeoutMS, TimeUnit.MILLISECONDS);
    }

    private void complete(boolean success) {
      if (future.complete(success)) {
        timerContext.stop();
        EndPointTask t = pendingTasks.remove(ep);
        Validate.isTrue(t == EndPointTask.this);
      }
    }

  }

}
