package py.common;

import py.common.struct.ExchangeMessage;

public interface ExchangeMessageCallback {
	public void onRecieved(ExchangeMessage message);

}
