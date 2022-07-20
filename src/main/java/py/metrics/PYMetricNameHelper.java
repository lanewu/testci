package py.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.exception.FormatIncorrectException;
import py.monitor.exception.UnsupportedIdentifierTypeException;
import py.monitor.jmx.server.ResourceType;
import py.monitor.utils.RegExUtils;

/**
 * this class is use for creating the metrics name by a specified template. we can get the resource type and resource
 * index out of this class
 * 
 * @author sxl
 *
 * @param <IdentifierDataType>
 *            the type of the "identifier" field, only support Long & String right now!
 */
public class PYMetricNameHelper<IdentifierDataType> implements java.io.Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PYMetricNameHelper.class);
    private static final long serialVersionUID = 699962755338453915L;
    private static final String METRICS_IDENTIFIER_PREFIX = "__<<TYPE[";
    private static final String REGEX_METRICS_IDENTIFIER_PREFIX = "__<<TYPE\\[";
    private static final String METRICS_IDENTIFIER_POSTFIX = "]>>";
    private static final String REGEX_METRICS_IDENTIFIER_POSTFIX = "\\]>>";
    private static final String SEPERATOR = "] ID[";
    private static final String REGEX_SEPERATOR = "\\] ID\\[";
    private static final String NAME_SEPERATOR = ".";

    private String name;
    private ResourceType type;
    private IdentifierDataType identifier;

    protected Class<IdentifierDataType> clazz;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public IdentifierDataType getIdentifier() {
        return identifier;
    }

    public void setIdentifier(IdentifierDataType identifier) {
        this.identifier = identifier;
    }

    public static String getMetricsIdentifierPrefix() {
        return METRICS_IDENTIFIER_PREFIX;
    }

    public static String getMetricsIdentifierPostfix() {
        return METRICS_IDENTIFIER_POSTFIX;
    }

    public static String getSeperator() {
        return SEPERATOR;
    }

    public static String getNameSeperator() {
        return NAME_SEPERATOR;
    }

    protected PYMetricNameHelper(String name, ResourceType type, IdentifierDataType identifier) {
        this.name = name;
        this.type = type;
        this.identifier = identifier;
    }

    @SuppressWarnings("unchecked")
    public PYMetricNameHelper(Class<IdentifierDataType> identifierType, String serializedIdentifier)
            throws FormatIncorrectException, UnsupportedIdentifierTypeException {
        if (serializedIdentifier == null) {
            logger.error("serialized metric name is not in correct format");
            throw new FormatIncorrectException();
        }

        String remainderString = serializedIdentifier;

        // get name
        String regex = "[\\w.*_#$%@!~&.<=\\{\\}\\?\\-\\^\\(\\)\\[\\]\\:]*" + REGEX_METRICS_IDENTIFIER_PREFIX;
        String subStr = RegExUtils.matchString(remainderString, regex);
        if (subStr == "") {
            this.name = serializedIdentifier;
            this.type = ResourceType.NONE;
            if (identifierType.getName().equals(Long.class.getName())) {
                identifier = (IdentifierDataType) Long.valueOf(0l);
            } else if (identifierType.getName().equals(String.class.getName())) {
                identifier = (IdentifierDataType) "0";
            } else {
                logger.error("Unsupported identifier type");
                throw new UnsupportedIdentifierTypeException();
            }
        } else {
            remainderString = remainderString.substring(subStr.length(), remainderString.length());
            logger.trace("Current remainder string is {}", remainderString);
            this.name = subStr.substring(0,
                    subStr.length() - METRICS_IDENTIFIER_PREFIX.length() - NAME_SEPERATOR.length());

            // get type
            regex = ResourceType.getRegex() + REGEX_SEPERATOR;
            subStr = RegExUtils.matchString(remainderString, regex);
            if (subStr == "") {
            } else {
                remainderString = remainderString.substring(subStr.length(), remainderString.length());
                logger.trace("Current remainder string is {}", remainderString);
                this.type = ResourceType.valueOf(subStr.substring(0, subStr.length() - SEPERATOR.length()));
            }

            // get identifier
            regex = "[\\w.*_#$%@!~&.<=\\{\\}\\?\\-\\^\\(\\)\\[\\]\\:]+" + REGEX_METRICS_IDENTIFIER_POSTFIX;
            subStr = RegExUtils.matchString(remainderString, regex);
            if (subStr == "") {
                logger.error("serialized identifier is not in correct format");
                throw new FormatIncorrectException();
            } else {
                remainderString = remainderString.substring(subStr.length(), remainderString.length());
                logger.trace("Current remainder string is {}, subStr is {}", remainderString, subStr);
                String identifierStr = subStr.substring(0, subStr.length() - METRICS_IDENTIFIER_POSTFIX.length());

                if (identifierType.getName().equals(Long.class.getName())) {
                    identifier = (IdentifierDataType) Long.valueOf(identifierStr);
                } else if (identifierType.getName().equals(String.class.getName())) {
                    identifier = (IdentifierDataType) identifierStr;
                } else {
                    logger.error("Unsupported identifier type");
                    throw new UnsupportedIdentifierTypeException();
                }
            }
        }
    }

    public String generate() throws UnsupportedIdentifierTypeException {
        String returnString = "";
        if ((type == null && identifier != null) || (type != null && identifier == null)) {
            logger.error("Unsupported identifier");
            throw new UnsupportedIdentifierTypeException();
        } else if (type == null && identifier == null) {
            logger.warn("This metrics is not assigned with a specified resouce");
            returnString = this.name;
        } else {
            returnString = this.name + NAME_SEPERATOR + METRICS_IDENTIFIER_PREFIX + type + SEPERATOR + identifier
                    + METRICS_IDENTIFIER_POSTFIX;
        }
        return returnString;
    }

    /**
     * this method return the name which will be used in the metrics repository
     * 
     * @return
     * @throws UnsupportedIdentifierTypeException
     */
    public String getAsRepositoryName() throws UnsupportedIdentifierTypeException {
        if ((type == null && identifier != null) || (type != null && identifier == null)) {
            logger.error("Unsupported identifier");
            throw new UnsupportedIdentifierTypeException();
        } else {
            return name;
        }
    }

    @Override
    public String toString() {
        return "ResourceIdentifier [name=" + name + ", type=" + type + ", identifier=" + identifier + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        @SuppressWarnings("rawtypes")
        PYMetricNameHelper other = (PYMetricNameHelper) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        return true;
    }
}
