package py.monitor.customizable.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeDescription {
    private static final Logger logger = LoggerFactory.getLogger(AttributeDescription.class);

    private String range;
    private String unitOfMeasurement;
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
