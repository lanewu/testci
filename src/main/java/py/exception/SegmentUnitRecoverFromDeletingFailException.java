package py.exception;

public class SegmentUnitRecoverFromDeletingFailException extends Exception {
    private static final long serialVersionUID = 1L;
    private long volumeId;
    private int segIndex;
    private String errorInfo;
    
    public SegmentUnitRecoverFromDeletingFailException(long volumeId, int segIndex, String errorInfo) {
        this.volumeId = volumeId;
        this.segIndex = segIndex;
        this.errorInfo = errorInfo;
    }
    
    public SegmentUnitRecoverFromDeletingFailException() {
        
    }
    
    public SegmentUnitRecoverFromDeletingFailException(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public int getSegIndex() {
        return segIndex;
    }

    public String getErrorInfo() {
        return errorInfo;
    }
}
