package py.app.thrift;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

public class ThriftUtils {

    public static void closeQuitely(TServiceClient client) {
        if (client != null) {
            TProtocol protocol = client.getOutputProtocol();
            if (protocol != null) {
                TTransport transport = protocol.getTransport();
                if (transport != null && transport.isOpen()) {
                    transport.close();
                }
            }
        }
    }
}
