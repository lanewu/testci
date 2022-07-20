package py.exception;

public class ArchiveNotExistException extends Exception {
    private static final long serialVersionUID = 1L;
    private long archiveId;
    private String errorInfo;

    public long getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(long archiveId) {
        this.archiveId = archiveId;
    }
    
    public ArchiveNotExistException(long archiveId) {
        this.archiveId = archiveId;
    }
    
    public ArchiveNotExistException(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
