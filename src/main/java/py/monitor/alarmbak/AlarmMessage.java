package py.monitor.alarmbak;

import java.io.Serializable;

import py.message.impl.Message;

public class AlarmMessage<T extends AlarmMessageData> extends Message<T> implements Serializable {
    private static final long serialVersionUID = 1L;

}
