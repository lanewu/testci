package py.monitor.customizable.repository;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import py.monitor.jmx.server.ResourceType;

@XmlRootElement(name = "AttributeMetadata")
@XmlType(propOrder = { "id", "resourceType", "mbeanObjectName", "attributeName", "attributeType", "range",
        "unitOfMeasurement", "description", "services" })
@XmlAccessorType(XmlAccessType.NONE)
public class AttributeMetadata implements Serializable {
    private static final long serialVersionUID = -5443796009223573969L;

    @XmlAttribute(name = "id")
    private long id;

    @XmlElement(name = "object-name")
    private String mbeanObjectName;

    @XmlElement(name = "resource-type")
    private ResourceType resourceType;

    @XmlElement(name = "attribute-name")
    private String attributeName;

    @XmlElement(name = "attribute-type")
    private String attributeType;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "range")
    private String range;

    @XmlElement(name = "unit-of-measurement")
    private String unitOfMeasurement;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "belongs-to")
    private Set<String> services;

    public AttributeMetadata() {
        services = new HashSet<String>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMbeanObjectName() {
        return mbeanObjectName;
    }

    public void setMbeanObjectName(String mbeanObjectName) {
        this.mbeanObjectName = mbeanObjectName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(String attributeType) {
        this.attributeType = attributeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    public void setUnitOfMeasurement(String unitOfMeasurement) {
        this.unitOfMeasurement = unitOfMeasurement;
    }

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
        result = prime * result + ((mbeanObjectName == null) ? 0 : mbeanObjectName.hashCode());
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
        AttributeMetadata other = (AttributeMetadata) obj;
        if (attributeName == null) {
            if (other.attributeName != null)
                return false;
        } else if (!attributeName.equals(other.attributeName))
            return false;
        if (mbeanObjectName == null) {
            if (other.mbeanObjectName != null)
                return false;
        } else if (!mbeanObjectName.equals(other.mbeanObjectName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AttributeMetadata [id=" + id + ", mbeanObjectName=" + mbeanObjectName + ", resourceType=" + resourceType
                + ", attributeName=" + attributeName + ", attributeType=" + attributeType + ", description="
                + description + ", range=" + range + ", unitOfMeasurement=" + unitOfMeasurement + ", services="
                + services + "]";
    }

}
