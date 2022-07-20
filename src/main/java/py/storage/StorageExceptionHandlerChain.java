package py.storage;

import java.util.ArrayList;
import java.util.List;

import py.exception.StorageException;

/**
 * A chain of handlers that will sequentially processing storage exception
 * 
 * @author chenlia
 */
public class StorageExceptionHandlerChain {
    private List<StorageExceptionHandler> handlers = new ArrayList<StorageExceptionHandler>();
    
    public void addHandler(StorageExceptionHandler handler) {
        handlers.add(handler);
    }
    
    public void processException(Storage storage, StorageException exception) {
        for (StorageExceptionHandler handler : handlers) {
            handler.handle(storage, exception);
        }
    }
}
