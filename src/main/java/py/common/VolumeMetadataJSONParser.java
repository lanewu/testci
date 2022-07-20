package py.common;

import org.apache.log4j.Logger;
/**
 * 
 * @author kobofare
 *
 */

public class VolumeMetadataJSONParser {
    private final Logger logger = Logger.getLogger(VolumeMetadataJSONParser.class);
    final private String volumeMetadataJSON;
    final private int version;
    
    public VolumeMetadataJSONParser(int version, String volumeMetadataJSON){
        this.version = version;
        this.volumeMetadataJSON = volumeMetadataJSON;
    }
    
    public VolumeMetadataJSONParser(String compositedVolumeMetadataJSON) {
        if (compositedVolumeMetadataJSON == null) {
            version = -1;
            volumeMetadataJSON = null;
        } else {
            int index = compositedVolumeMetadataJSON.indexOf(':');
            if (index == -1) {
                logger.warn("can not parse the volumeMetadataJSON:" + compositedVolumeMetadataJSON);
                version = -1;
                volumeMetadataJSON = null;
            } else {
                version = Integer.valueOf(compositedVolumeMetadataJSON.substring(0,index));
                volumeMetadataJSON = compositedVolumeMetadataJSON.substring(index + 1);
            }
        }
    }
    
    public String getCompositedVolumeMetadataJSON() {
        return version + ":" + volumeMetadataJSON;
    }
    
    public String getVolumeMetadataJSON() {
        return volumeMetadataJSON;
    }

    public int getVersion() {
        return version;
    }
}
