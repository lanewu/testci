package py.monitor.jmx.server;

import java.io.Serializable;

import py.monitor.customizable.repository.AttributeMetadata;

public class ResourceRelatedAttribute implements Serializable {
    private static final long serialVersionUID = 8007782467342061084L;

    private AttributeMetadata attributeMetadata;
    private String resourceId;

    public AttributeMetadata getAttributeMetadata() {
        return attributeMetadata;
    }

    public void setAttributeMetadata(AttributeMetadata attributeMetadata) {
        this.attributeMetadata = attributeMetadata;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "ResourceRelatedAttribute [attributeMetadata=" + attributeMetadata + ", resourceId=" + resourceId + "]";
    }

}
