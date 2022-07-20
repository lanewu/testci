package py.common.struct;

import io.netty.buffer.ByteBuf;
import io.netty.util.Timeout;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class EchoFutureMessage {
	
    private Timeout receiveTimeout;
    private final SettableFuture<ByteBuf> response;
	
	
	public EchoFutureMessage(){
		this.response = SettableFuture.create();
	}
    
	public void onResponseReceived(ByteBuf response)
    {
        this.response.set(response);
    }
	
	public ListenableFuture<ByteBuf> getResponse()
    {
        return response;
    } 
	
	public void setTimeout(Timeout rcvtimeout){
		receiveTimeout= rcvtimeout;
	}
	
	public void clearTimeout(){
		receiveTimeout.cancel();
	}
	
	public boolean isArrived(){
		return(response.isDone());
	}
		
	public byte[] readAvailableByteArray(int waitMills) {
		ByteBuf chbuffer=null;
		try {
			chbuffer = response.get(waitMills,TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
		
		int len = chbuffer.readableBytes();
		
		byte[] srcdata = chbuffer.readBytes(len).array();
		return srcdata;
	}
	
	public ByteBuffer readAvailableByteBuffer(int waitMills) {
		ByteBuf chbuffer=null;
		try {
			chbuffer = response.get(waitMills,TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
		
		int len = chbuffer.readableBytes();
		
		return chbuffer.readBytes(len).nioBuffer(0, len);
	}
	
	public void releaseBuffer(){
		ByteBuf chbuffer=null;
		try {
			chbuffer = response.get();
		} catch (Exception e) {
			//e.printStackTrace();
			return ;
		}
		if(chbuffer!=null)
			chbuffer.release();
	}
	
}
