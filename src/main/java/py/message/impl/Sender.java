package py.message.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.message.IMessage;
import py.message.IMessageHandler;
import py.message.IReceiver;
import py.message.ISender;
import py.message.exceptions.MessageHandlerMissingException;
import py.message.exceptions.MessageMapMissingException;

/**
 * 
 * @author sxl
 *
 */

public class Sender implements ISender {
    private static final Logger logger = LoggerFactory.getLogger(Sender.class);
    protected MessageDispatcher dispatcher = MessageDispatcher.getInstance();

    public Sender() {
        dispatcher.start();
    }

    @Override
    public void sendMessage(IMessage<?> message, IReceiver receiver)
            throws MessageMapMissingException, MessageHandlerMissingException, Exception {
        Map<Class<?>, IMessageHandler> destObjMessageMap = receiver.getMessageMap();
        if (destObjMessageMap == null) {
            throw new MessageMapMissingException();
        }

        IMessageHandler handler = destObjMessageMap.get(message.getClass());
        if (handler == null) {
            throw new MessageHandlerMissingException();
        } else {
            try {
                handler.execute(message);
            } catch (Exception e) {
                logger.error("Caught an exception when deal with message: {}", message, e);
                throw e;
            }
        }
    }

    @Override
    public void postMessage(IMessage<?> message, IReceiver receiver) {
        logger.debug("Message name: {}", message.getClass());
        this.dispatcher.add(message, receiver);
    }

    @Override
    public void postMessage(IMessage<?> message) {
        logger.debug("Going to broadcast message: {}", message);
        this.dispatcher.add(message);
    }

    @Override
    public String toString() {
        return "Sender [dispatcher=" + dispatcher + "]";
    }

}
