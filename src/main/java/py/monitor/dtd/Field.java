package py.monitor.dtd;

import java.io.Serializable;

import org.springframework.util.SerializationUtils;

import py.common.Utils;
import py.monitor.jmx.server.ResourceType;

public class Field implements Serializable {
    private static final long serialVersionUID = 6902390018918642226L;
    private FieldID id;
    private String name;
    private String type;
    private byte[] data;
    private String dataSourceProvider;
    private long timeStamp;
    private ResourceType resourceType;
    private String resourceId;

    public FieldID getId() {
        return id;
    }

    public void setId(FieldID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getDataSourceProvider() {
        return dataSourceProvider;
    }

    public void setDataSourceProvider(String subTaskName) {
        this.dataSourceProvider = subTaskName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "Field [id=" + id + ", name=" + name + ", type=" + type + ", data="
                + SerializationUtils.deserialize(data) + ", dataSourceProvider=" + dataSourceProvider + ", timeStamp="
                + timeStamp + "(" + Utils.millsecondToString(timeStamp) + "), resourceType=" + resourceType
                + ", resourceId=" + resourceId + "]";
    }

}
