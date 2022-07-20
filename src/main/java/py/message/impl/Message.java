package py.message.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.message.IMessage;

public class Message<DataType> implements IMessage<DataType> {
    private static final Logger logger = LoggerFactory.getLogger(Message.class);
    protected final UUID uuid = UUID.randomUUID();
    protected String name;
    protected DataType data;

    public Message(String name, DataType data) {
        this.name = name;
        this.data = data;
    }

    public Message(DataType data) {
        this.name = this.getClass().getName();
    }

    public Message() {
        this.name = this.getClass().getName();
        logger.warn("You've invoke the constructor of [{}] which is not going to initialize the message data."
                + "So you have to use setter to set the message data, or the data will be null", name);
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DataType getData() {
        return data;
    }

    @Override
    public void setData(DataType data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message [uuid=" + uuid + ", name=" + name + ", data=" + data + "]";
    }

}
