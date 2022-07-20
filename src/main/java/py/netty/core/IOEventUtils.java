package py.netty.core;

public class IOEventUtils {
  public static int calculateThreads(IOEventThreadsMode threadsMode, Float parameter) {
    switch (threadsMode) {
      case Calculate_From_Available_Core:
        return calculateThreadsByAvailableCores(parameter);
      case Fix_Threads_Mode:
        return parameter.intValue();
      default:
        return Runtime.getRuntime().availableProcessors();
    }
  }

  private static int calculateThreadsByAvailableCores(Float parameter) {
    int cores = Runtime.getRuntime().availableProcessors();
    return Math.max(1, (int)(parameter * cores));
  }
}
