package py.instance;

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import py.common.FrequentlyUsedStrings;

/***
 * Location stores the location of an instance. The cluster and dc (group) of a location can't be empty. 
 * 
 * The location should look like: 
 * c(luster)=xx;d(c)=xx;r(ack)=xx;h(ost)=xx
 * 
 * chars within parentheses do not present in the string. 
 */
public class Location implements Serializable {
    
    private static final long serialVersionUID = 351334138100567343L;
    private static final String CLUSTER_FIELD_NAME = "c";
    private static final String DC_FIELD_NAME = "d";
    private static final String RACK_FIELD_NAME = "r";
    private static final String HOST_FIELD_NAME = "h";
    private static final String LOCATION_FIELD_NAME = "l";

    // similar to Region
    private String cluster; 
    // similar to datacenter
    private String dc; 
    private String rack;
    private String host; 
    
    public Location(Location other) {
        this.cluster = other.cluster;
        this.dc = other.dc;
        this.rack = other.rack;
        this.host = other.host;
    }

    public Location(String cluster, String dc) {
        this(cluster, dc, null, null);
    }
    
    
    public Location(String cluster, String dc, String rack, String host) {
        if (cluster == null || cluster.isEmpty()) 
            throw new IllegalArgumentException("Cluster can't be null");
        if (dc == null || dc.isEmpty()) 
            throw new IllegalArgumentException("DC can't be null");
        this.cluster = FrequentlyUsedStrings.get(cluster);
        this.dc = FrequentlyUsedStrings.get(dc);
        // use 'new String()' to make sure we aren't sharing original backing char[]
        this.rack = rack==null ? null : new String(rack);
        this.host = host==null ? null : new String(host);
    }

    @Override
    public String toString() {
       // cluster=xx;dc=xx;rack=xx;host=xx
        StringBuffer sb = new StringBuffer(32);
        sb.append(CLUSTER_FIELD_NAME).append("=").append(cluster).append(";");
        sb.append(DC_FIELD_NAME).append("=").append(dc);
        if (rack != null) sb.append(";").append(RACK_FIELD_NAME).append("=").append(rack);
        if (host != null) sb.append(";").append(HOST_FIELD_NAME).append("=").append(host);
        return sb.toString();
    }
    
   @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
        result = prime * result + ((dc == null) ? 0 : dc.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((rack == null) ? 0 : rack.hashCode());
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
        Location other = (Location) obj;
        if (cluster == null) {
            if (other.cluster != null)
                return false;
        } else if (!cluster.equals(other.cluster))
            return false;
        if (dc == null) {
            if (other.dc != null)
                return false;
        } else if (!dc.equals(other.dc))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (rack == null) {
            if (other.rack != null)
                return false;
        } else if (!rack.equals(other.rack))
            return false;
        return true;
    }
    
    public static Location fromString(String lstr) {
        if (lstr == null) {
            return null;
        }
        // since we parse this up, make sure we aren't reusing a larger backing char[]
        lstr = new String(lstr);

        int pos = 0;
        final int end = lstr.length();
        String cluster = null;
        String group = null;
        String rack = null;
        String host = null;
        while (pos < end) {
            int start = pos;
            while (pos < end && lstr.charAt(pos) != '=')
                pos++;
            if (pos == end)
                throw new IllegalArgumentException("Syntax error in location: " + lstr);
            String name = lstr.substring(start, pos);
            pos++; // skip =
            start = pos;
            while (pos < end && lstr.charAt(pos) != ';')
                pos++;
            String value = lstr.substring(start, pos);
            pos++; // skip ;

            if (cluster == null && CLUSTER_FIELD_NAME.equals(name))
                cluster = value;
            else if (group == null && DC_FIELD_NAME.equals(name))
                group = value;
            else if (rack == null && RACK_FIELD_NAME.equals(name))
                rack = value;
            else if (host == null && HOST_FIELD_NAME.equals(name))
                host = value;
            else if (CLUSTER_FIELD_NAME.equals(name)
                    || DC_FIELD_NAME.equals(name)
                    || RACK_FIELD_NAME.equals(name)
                    || HOST_FIELD_NAME.equals(name))
                throw new IllegalArgumentException("Duplicate field in location: " + lstr);
        }

        return new Location(cluster, group, rack, host);
    }
     
    public String getHost() { return this.host; }
    public String getDc() { return this.dc; }
    public String getRack() { return this.rack; }
    public String getCluster() { return this.cluster;}

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public void setDc(String dc) {
        this.dc = dc;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public void setHost(String host) {
        this.host = host;
    }
    
    public static class LocationDeserializer extends StdDeserializer<Location> {

        private static final long serialVersionUID = 913054104891080L;

        protected LocationDeserializer() {
            super(Location.class);
        }

        @Override
        public Location deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            JsonNode locationNode = node.get(LOCATION_FIELD_NAME);
            if (locationNode == null) {
                throw new IOException("Can't find location field");
            }

            String locationString = locationNode.asText();
            if(locationString == null) {
               throw new IOException("location can't be empty");
            }

            return Location.fromString(locationString);
        }
    }
    
    public static class LocationSerializer extends StdSerializer<Location> {

        public LocationSerializer() {
            super(Location.class, true);
        }

        @Override
        public void serialize(Location value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField(LOCATION_FIELD_NAME, value.toString());
            jgen.writeEndObject();
        }
    }
}
