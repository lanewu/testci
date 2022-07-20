package py.json.socket;

/**
 * A class to create instance of JsonSocketClient.
 * 
 * @author zjm
 *
 */
public class JsonSocketClientFactory extends JsonSocketFactory {
    /*
     * use the volume id to create sock name
     */
    private long volumeId;

    public JsonSocketClient createJsonSocketClient() {
        String sockName = genSockNameFromVolumeId(volumeId);

        JsonSocketClient jsonSocketClient = new JsonSocketClient();
        jsonSocketClient.setSockName(sockName);

        return jsonSocketClient;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }
}
