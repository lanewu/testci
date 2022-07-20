package py.exception;

import py.exception.StorageException;

public class LatencyTooLargeException extends StorageException {
    public LatencyTooLargeException() {
        ioException = true;
    }
}


