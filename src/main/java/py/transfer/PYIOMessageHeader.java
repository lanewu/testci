package py.transfer;

public class PYIOMessageHeader {

    private int magic;
    private int length;
    private long requestid;
    private int channelid;

    public PYIOMessageHeader(int magic, int length, long requestid, int channelid) {
        this.magic = magic;
        this.length = length;
        this.requestid = requestid;
        this.channelid = channelid;
    }

    public long getRequestid() {
        return requestid;
    }

    public void setRequestid(long requestid) {
        this.requestid = requestid;
    }

    public PYIOMessageHeader() {

    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public int getChannelid() {
        return channelid;
    }

    public void setChannelid(int channelid) {
        this.channelid = channelid;
    }

    @Override
    public Object clone() {
        return new PYIOMessageHeader(this.magic, this.length, this.requestid, this.channelid);
    }

    @Override
    public String toString() {
        return "PYIOMessageHeader [magic=" + magic + ", length=" + length + ", requestid=" + requestid + ", channelid="
                + channelid + "]";
    }

}
