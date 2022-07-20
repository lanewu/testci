package py.message;

/**
 * This interface defines a handler to deal with the message{@link IMessage}
 * 
 * @author sxl
 * 
 */
public interface IMessageHandler {
    public void execute(IMessage<?> message) throws Exception;
}
