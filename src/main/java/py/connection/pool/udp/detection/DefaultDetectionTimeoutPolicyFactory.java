package py.connection.pool.udp.detection;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultDetectionTimeoutPolicyFactory implements DetectionTimeoutPolicyFactory {

  private final int maxRetryTimes;
  private final long initTimeoutMS;

  public DefaultDetectionTimeoutPolicyFactory(int maxRetryTimes, long initTimeoutMS) {
    this.maxRetryTimes = maxRetryTimes;
    this.initTimeoutMS = initTimeoutMS;
  }

  @Override
  public DetectionTimeoutPolicy generate() {
    return new DetectionTimeoutPolicy() {

      AtomicInteger currentRetryTimes = new AtomicInteger(1);

      @Override
      public long getTimeoutMS() {
        int time = currentRetryTimes.getAndIncrement();

        if (time > maxRetryTimes) {
          return -1;
        }

        return initTimeoutMS * time;
      }
    };
  }


}
