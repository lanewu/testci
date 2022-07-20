package py.connection.pool.udp.detection;

public interface DetectionTimeoutPolicy {

  /**
   * Get next timeout
   *
   * @return next timeout in MS or -1 if timed out
   */
  long getTimeoutMS();

}
