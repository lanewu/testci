package py.instance;

/**
 * A class as identifier of group. In distributed system, instances of service are running on different machines,
 * different location, different area. For example, datanode service run on machines with ips 10.10.10.10, 10.10.10.11,
 * ...; locations BeiJing, ShangHai, GuangZhou, ...; areas Asia, Europe, ... To manage these instances, group is
 * required.
 * 
 * @author zjm
 *
 */
public class Group {
    public Group() {
        
    }
    public Group(int groupId) {
        super();
        this.groupId = groupId;
    }
    
    public Group(Group other) {
        this.groupId = other.groupId;
    }

    private int groupId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + groupId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return true;
        }

        Group otherGroup = (Group) obj;
        if (groupId != otherGroup.getGroupId()) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Group [groupId=" + groupId + "]";
    }
}
