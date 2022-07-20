package py.message;

import java.util.Map;

import py.message.exceptions.MessageMapMissingException;

/**
 * {@link IReceiver} is used to define a type of object who can deal with the message
 * 
 * @author sxl
 * 
 */
public interface IReceiver {
    /**
     * this method is uesed to define a message map in message receiver who is going to deal with the message.
     * 
     * @throws Exception
     */
    public void buildMessageMap() throws Exception;

    /**
     * get the message reference out.
     * 
     * @return Message map that has been built
     * @throws MessageMapMissingException
     */
    public Map<Class<?>, IMessageHandler> getMessageMap() throws MessageMapMissingException;

    /**
     * regist the receiver to {@code PyMessageDispatcher}
     * 
     */
    public void regist();

    /**
     * unregist receiver from {@code PyMessageDispatcher}
     * 
     */
    public void unregist();
}
