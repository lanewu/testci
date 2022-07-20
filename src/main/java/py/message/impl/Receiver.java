package py.message.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.message.IMessageHandler;
import py.message.IReceiver;
import py.message.exceptions.MessageMapMissingException;

/**
 * 
 * @author sxl
 * 
 */
public abstract class Receiver implements IReceiver {
    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);
    protected MessageDispatcher dispatcher = MessageDispatcher.getInstance();
    protected Map<Class<?>, IMessageHandler> messageMap = null;

    public Receiver() {
        dispatcher.start();
        messageMap = new HashMap<Class<?>, IMessageHandler>();
        try {
            buildMessageMap();

            // register as a listener
            this.regist();
        } catch (Exception e) {
            logger.error("Caught an exception when built message map", e);
        }
    }

    @Override
    public Map<Class<?>, IMessageHandler> getMessageMap() throws MessageMapMissingException {
        if (messageMap == null) {
            throw new MessageMapMissingException();
        }
        return messageMap;
    }

    @Override
    public synchronized void regist() {
        dispatcher.addLisener(this);
    }

    @Override
    public synchronized void unregist() {
        dispatcher.removeLisener(this);
    }

    @Override
    public String toString() {
        return "Receiver [dispatcher=" + dispatcher + ", messageMap=" + messageMap + "]";
    }

}
