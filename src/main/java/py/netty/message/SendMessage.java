package py.netty.message;

import com.google.protobuf.AbstractMessage;
import io.netty.buffer.ByteBuf;
import org.apache.hadoop.classification.InterfaceAudience;
import py.netty.core.MethodCallback;

/**
 * Created by kobofare on 2017/2/17.
 */
public class SendMessage extends MessageImpl {

    private final MethodCallback<AbstractMessage> callback;

    public SendMessage(Header header, ByteBuf buffer, MethodCallback<AbstractMessage> callback) {
        super(header, buffer);
        this.callback = callback;
    }

    public MethodCallback<AbstractMessage> getCallback() {
        return callback;
    }
}
