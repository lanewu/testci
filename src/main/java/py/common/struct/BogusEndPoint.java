package py.common.struct;

public class BogusEndPoint extends EndPoint {
    private static Integer unavailablePort = -1;
    public BogusEndPoint() {
        setHostName("InvalidHostName");
        setPort(unavailablePort--);
    }

}
