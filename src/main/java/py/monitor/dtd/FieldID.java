package py.monitor.dtd;

import java.io.Serializable;
import java.util.UUID;

/**
 * 
 * @author sxl
 * 
 */
public class FieldID implements Serializable {
    private static final long serialVersionUID = -221542078953395087L;

    /**
     * <code>taskId</code> is the id of the task which hasn't been divided into sub-tasks.<br>
     * we define this attribute as a part of the {@code FieldId} here for these two reasons: <br>
     * 
     * 1. we need to known where's the field data from because there might be a lots of field data for the same
     * performance item. the <code>taskId</code> can tell us this information.
     * 
     * 2. we need this attribute to get the alarm expression to make the alarms.
     */
    private long taskId;

    /**
     * <code>metadataNumber</code> is defined in the performance & alert item meta data repository called
     * {@code AttributeStore}, we use this number to specify the type of the filed
     */
    private long metadataNumber;

    /**
     * <code>dataIndex</code> is the field data index, we set a index for a each field data in order to create the
     * 'view' based on the {@code ITask} and attribute.
     */
    private UUID dataIndex;

    public FieldID(long taskId, long metadataNumber, UUID dataIndex) {
        this.taskId = taskId;
        this.metadataNumber = metadataNumber;
        this.dataIndex = dataIndex;
    }

    public long getMetadataNumber() {
        return metadataNumber;
    }

    public void setMetadataNumber(long fieldMetadataNumber) {
        this.metadataNumber = fieldMetadataNumber;
    }

    public UUID getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(UUID dataIndex) {
        this.dataIndex = dataIndex;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataIndex == null) ? 0 : dataIndex.hashCode());
        result = prime * result + (int) (metadataNumber ^ (metadataNumber >>> 32));
        result = prime * result + (int) (taskId ^ (taskId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FieldID other = (FieldID) obj;
        if (dataIndex == null) {
            if (other.dataIndex != null)
                return false;
        } else if (!dataIndex.equals(other.dataIndex))
            return false;
        if (metadataNumber != other.metadataNumber)
            return false;
        if (taskId != other.taskId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return metadataNumber + ":" + dataIndex;
    }

}
