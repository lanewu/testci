package py.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Configuration for netty server and client
 *
 */
@Configuration
@PropertySource("classpath:config/netty.properties")
public class NettyConfiguration {
  @Value("${netty.max.buf.length.for.allocate.adapter:32768}")
  private int nettyMaxBufLengthForAllocateAdapter = 32768;

  /**
   * Thread mode refers to the calculation mode for calculating the maximum number of threads
   * in the thread pool when configuring the thread pool for netty.
   * Currently, there are two types of calculation modes:
   *  0 is fixed configuration
   *  1 is based on the number of cpu cores
   *
   * Netty needs to set up two thread pools,
   * namely IO thread pool for data reading and writing and business processing thread pool.
   * We set configuration variables for the two thread pools respectively.
   *
   * Because the processing models of the server and the client are different,
   * differentiated configurations are also carried out.
   */
  @Value("${netty.server.io.event.group.threads.mode:0}")
  private int nettyServerIoEventGroupThreadsMode = 0;
  @Value("${netty.server.io.event.group.threads.parameter:2}")
  private float nettyServerIoEventGroupThreadsParameter = 2;
  @Value("${netty.server.io.event.handle.threads.mode:0}")
  private int nettyServerIoEventHandleThreadsMode = 0;
  @Value("${netty.server.io.event.handle.threads.parameter:4}")
  private float nettyServerIoEventHandleThreadsParameter = 4;

  @Value("${netty.client.io.event.group.threads.mode:0}")
  private int nettyClientIoEventGroupThreadsMode = 0;
  @Value("${netty.client.io.event.group.threads.parameter:2}")
  private float nettyClientIoEventGroupThreadsParameter = 2;
  @Value("${netty.client.io.event.handle.threads.mode:0}")
  private int nettyClientIoEventHandleThreadsMode = 0;
  @Value("${netty.client.io.event.handle.threads.parameter:4}")
  private float nettyClientIoEventHandleThreadsParameter = 4;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  public int getNettyMaxBufLengthForAllocateAdapter() {
    return nettyMaxBufLengthForAllocateAdapter;
  }

  public void setNettyMaxBufLengthForAllocateAdapter(int nettyMaxBufLengthForAllocateAdapter) {
    this.nettyMaxBufLengthForAllocateAdapter = nettyMaxBufLengthForAllocateAdapter;
  }

  public int getNettyServerIoEventGroupThreadsMode() {
    return nettyServerIoEventGroupThreadsMode;
  }

  public void setNettyServerIoEventGroupThreadsMode(int nettyServerIoEventGroupThreadsMode) {
    this.nettyServerIoEventGroupThreadsMode = nettyServerIoEventGroupThreadsMode;
  }

  public float getNettyServerIoEventGroupThreadsParameter() {
    return nettyServerIoEventGroupThreadsParameter;
  }

  public void setNettyServerIoEventGroupThreadsParameter(
      float nettyServerIoEventGroupThreadsParameter) {
    this.nettyServerIoEventGroupThreadsParameter = nettyServerIoEventGroupThreadsParameter;
  }

  public int getNettyClientIoEventGroupThreadsMode() {
    return nettyClientIoEventGroupThreadsMode;
  }

  public void setNettyClientIoEventGroupThreadsMode(int nettyClientIoEventGroupThreadsMode) {
    this.nettyClientIoEventGroupThreadsMode = nettyClientIoEventGroupThreadsMode;
  }

  public float getNettyClientIoEventGroupThreadsParameter() {
    return nettyClientIoEventGroupThreadsParameter;
  }

  public void setNettyClientIoEventGroupThreadsParameter(
      float nettyClientIoEventGroupThreadsParameter) {
    this.nettyClientIoEventGroupThreadsParameter = nettyClientIoEventGroupThreadsParameter;
  }

  public int getNettyServerIoEventHandleThreadsMode() {
    return nettyServerIoEventHandleThreadsMode;
  }

  public void setNettyServerIoEventHandleThreadsMode(int nettyServerIoEventHandleThreadsMode) {
    this.nettyServerIoEventHandleThreadsMode = nettyServerIoEventHandleThreadsMode;
  }

  public float getNettyServerIoEventHandleThreadsParameter() {
    return nettyServerIoEventHandleThreadsParameter;
  }

  public void setNettyServerIoEventHandleThreadsParameter(
      float nettyServerIoEventHandleThreadsParameter) {
    this.nettyServerIoEventHandleThreadsParameter = nettyServerIoEventHandleThreadsParameter;
  }

  public int getNettyClientIoEventHandleThreadsMode() {
    return nettyClientIoEventHandleThreadsMode;
  }

  public void setNettyClientIoEventHandleThreadsMode(int nettyClientIoEventHandleThreadsMode) {
    this.nettyClientIoEventHandleThreadsMode = nettyClientIoEventHandleThreadsMode;
  }

  public float getNettyClientIoEventHandleThreadsParameter() {
    return nettyClientIoEventHandleThreadsParameter;
  }

  public void setNettyClientIoEventHandleThreadsParameter(
      float nettyClientIoEventHandleThreadsParameter) {
    this.nettyClientIoEventHandleThreadsParameter = nettyClientIoEventHandleThreadsParameter;
  }
}
