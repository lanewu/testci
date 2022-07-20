package py.common.struct;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EchoMessage {
	private ByteBuffer  bytebuffer;
	private int lenData;
	private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    
	public int getDatalen(){
		return lenData;
	}
	
	public void setDatalen(int length){
		lenData = length;
	}
	
	public EchoMessage(ByteBuffer  bytebuffer){
		this.bytebuffer = bytebuffer;
		this.lenData = 0;
	}
	
	public ByteBuffer getBuffer(){
		return bytebuffer;
	}
	
	public void lock(){
		lock.lock();
	}
	
	public void unlock(){
		lock.unlock();
	}
	
	public void await(int delay){
		try {
			condition.await(delay,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void signal(){
		condition.signal();
	}
}
