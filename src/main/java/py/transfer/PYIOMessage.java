package py.transfer;

import io.netty.buffer.ByteBuf;

/*
 * define message transfered between datanode and cordinator 
 */
public class PYIOMessage {
    final public static int MAGIC_REQUEST = 0x48282150;
    final public static int MAGIC_RESPONSE = 0x48282151;

    private PYIOMessageHeader header;
    private ByteBuf body;

    public PYIOMessageHeader getHeader() {
        return header;
    }

    public void setHeader(PYIOMessageHeader header) {
        this.header = header;
    }

    public ByteBuf getBody() {
        return body;
    }

    public void setBody(ByteBuf body) {
        this.body = body;
    }

    public interface PYIOMessageContent {
        public ByteBuf getData();

        public void setData(ByteBuf data);
    }

}
