package py.netty.core;

public enum IOEventThreadsMode {
  Fix_Threads_Mode(0),
  Calculate_From_Available_Core(1);

  int value;

  IOEventThreadsMode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static IOEventThreadsMode findByValue(int value) {
    switch (value) {
      case 0:
        return Fix_Threads_Mode;
      case 1:
        return Calculate_From_Available_Core;
      default:
        return null;
    }
  }
}
