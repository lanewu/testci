package py.storage;

import py.exception.StorageException;

public interface StorageExceptionHandler {
    /**
     * Handle the storage exception raised by storage read/write.  
     * @param storage
     * @param exception
     * @return true to allow the next handle to continue to process the exception
     */
    void handle(Storage storage, StorageException exception);

}
