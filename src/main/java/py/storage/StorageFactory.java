package py.storage;

import py.exception.StorageException;

public interface StorageFactory {
    public Storage generate(String id) throws StorageException;
}
