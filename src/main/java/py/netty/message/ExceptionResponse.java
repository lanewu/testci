package py.netty.message;

public class ExceptionResponse {
    private final ResponseHeader responseHeader;
    private String decription;

    public ExceptionResponse(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public String getDecription() {
        return decription;
    }

    public void setDecription(String decription) {
        this.decription = decription;
    }

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

}
