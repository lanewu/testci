package py.message;

import java.util.UUID;

/**
 * This interface defines a message which will be send by {@code Sender or Messager}, and
 * then be dispatched to the message map that belongs to another {@code Sender or Messager} or itself.
 * 
 * After that, the {@code PyMessage} will be dispatched to each {@code IMessageHandler} by the rules that is defined in
 * the message map
 * 
 * @author sxl
 * 
 * @param <DataType>
 *            the type of message date.
 */
public interface IMessage<DataType> {
    /**
     * 
     * @return
     */
    public UUID uuid();

    /**
     * 
     * @return
     */
    public String name();

    /**
     * 
     * @return
     */
    public DataType getData();

    /**
     * 
     * @param data
     */
    public void setData(DataType data);
}
