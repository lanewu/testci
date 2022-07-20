package py.common;

import py.common.struct.EndPoint;

public interface UdpTransfer {
	/** 
	 * this method send data to the endpoint by udp socket
	 * @param bytedata
	 * @param endPoint
	 */
	public void writeBytes(byte[] bytedata,EndPoint endPoint);
	public void writeBytes(byte[] bytedata,int length, EndPoint endPoint);
	
	/**
	 * this method read data from udp socket
	 * @param bytedata
	 * @return actual read data length
	 */
	public int readBytes(byte[] bytedata);
	public int readBytes(byte[] bytedata,int length);

}
