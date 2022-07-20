package py.message;

import py.message.exceptions.MessageHandlerMissingException;
import py.message.exceptions.MessageMapMissingException;

/**
 * 
 * @author sxl
 * 
 */
public interface ISender {
    /**
     * sendMessage just send the message to a {@link IMessageHandler}.So,it's a SYNCHRONIZED method
     * 
     * @param message
     * @param receiver
     * @throws MessageMapMissingException
     * @throws MessageHandlerMissingException
     * @throws Exception
     */
    public void sendMessage(IMessage<?> message, IReceiver receiver)
            throws MessageMapMissingException, MessageHandlerMissingException, Exception;

    /**
     * postMessage will send the message to the {@link py.message.impl.MessageDispatcher}, and will be dispatch to a
     * {@link IMessageHandler}.So,it's a ASYNCHRONIZED method
     * 
     * @param message
     * @param receiver
     */
    public void postMessage(IMessage<?> message, IReceiver receiver);

    /**
     * broadcast the message to all objects the had been registered to the {@link py.message.impl.MessageDispatcher}
     * 
     * @param message
     */
    public void postMessage(IMessage<?> message);
}
