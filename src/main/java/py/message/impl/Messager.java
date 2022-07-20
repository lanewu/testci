package py.message.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.message.IMessage;
import py.message.IReceiver;
import py.message.ISender;
import py.message.exceptions.MessageHandlerMissingException;
import py.message.exceptions.MessageMapMissingException;

/**
 * I made this class to combine the function of {@code Sender} and {@code Receiver} together.
 * 
 * @author sxl
 * 
 */
public abstract class Messager extends Receiver implements ISender {
    private static final Logger logger = LoggerFactory.getLogger(Messager.class);
    protected MessageDispatcher dispatcher = MessageDispatcher.getInstance();

    public Messager() {
        super();
    }

    @Override
    public void sendMessage(IMessage<?> message, IReceiver receiver)
            throws MessageMapMissingException, MessageHandlerMissingException, Exception {
        Sender sender = new Sender();
        sender.sendMessage(message, receiver);
    }

    @Override
    public void postMessage(IMessage<?> message, IReceiver receiver) {
        Sender sender = new Sender();
        sender.postMessage(message, receiver);
    }

    @Override
    public void postMessage(IMessage<?> message) {
        logger.debug("Going to broadcast message: {}", message);
        this.dispatcher.add(message);
    }

    @Override
    public String toString() {
        return "MessageObject [dispatcher=" + dispatcher + ", messageMap=" + messageMap + "]";
    }

}