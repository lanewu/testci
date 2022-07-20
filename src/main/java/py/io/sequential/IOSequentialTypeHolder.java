package py.io.sequential;

public interface IOSequentialTypeHolder {
  public enum IOSequentialType {
    UNKNOWN(0),
    SEQUENTIAL_TYPE(1),
    RANDOM_TYPE(2),
    ;
    private int val;
    IOSequentialType(int val) {
      this.val = val;
    }
    public int val() {return val;}
  }

  long getOffset();

  int getLength();

  IOSequentialType getIoSequentialType();

  void setIoSequentialType(IOSequentialType ioSequentialType);
}
