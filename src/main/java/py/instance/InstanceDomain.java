package py.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * only use to target datanode value
 * 
 * @author wangzy
 * 
 */
public class InstanceDomain {
    private Long domainId;

    public InstanceDomain() {
        this.domainId = null;
    }

    public InstanceDomain(Long domainId) {
        this.setDomainId(domainId);
    }

    public InstanceDomain(InstanceDomain other) {
        this.domainId = other.domainId;
    }

    /**
     * when an instance has no domainId, means it is free
     * 
     * @return
     */
    @JsonIgnore
    public boolean isFree() {
        return this.domainId == null;
    }

    /**
     * set an instance node free
     */
    @JsonIgnore
    public void setFree() {
        this.domainId = null;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((domainId == null) ? 0 : domainId.hashCode());
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
        InstanceDomain other = (InstanceDomain) obj;
        if (domainId == null) {
            if (other.domainId != null)
                return false;
        } else if (!domainId.equals(other.domainId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DomainId [domainId=" + domainId + "]";
    }

}
