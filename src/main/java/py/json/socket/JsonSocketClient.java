package py.json.socket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

/**
 * A class to parse json from socket.
 * 
 * @author zjm
 *
 */
public class JsonSocketClient implements JsonSocket {
    /*
     * an id of socket, client and server have the same socket name
     */
    private String sockName;

    public String parseJsonFromSocket() throws IOException {

        final File socketFile = new File(new File(System.getProperty("java.io.tmpdir")), sockName);

        AFUNIXSocket sock = null;
        InputStream is = null;

        try {
            sock = AFUNIXSocket.newInstance();
            sock.connect(new AFUNIXSocketAddress(socketFile));

            is = sock.getInputStream();

            byte[] headBytes = new byte[HEAD_LEN];
            int headReadLen = 0;
            while (headReadLen < HEAD_LEN) {
                headReadLen += is.read(headBytes, headReadLen, HEAD_LEN - headReadLen);
            }

            int jsonLen = ByteBuffer.wrap(headBytes).getInt();
            byte[] jsonBytes = new byte[jsonLen];
            int jsonReadLen = 0;
            while (jsonReadLen < jsonLen) {
                jsonReadLen += is.read(jsonBytes, jsonReadLen, jsonLen - jsonReadLen);
            }
            return new String(jsonBytes, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new IOException("IOException occurred when parse json from socket");
        } finally {
            if (is != null) {
                is.close();
            }
            if (sock != null) {
                sock.close();
            }
        }

    }

    public String getSockName() {
        return sockName;
    }

    public void setSockName(String sockName) {
        this.sockName = sockName;
    }
}
