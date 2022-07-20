package py.storage.impl;

import py.exception.StorageException;
import py.storage.Storage;
import py.storage.StorageFactory;

public class DummyStorageFactory implements StorageFactory {
    private int size;

    public StorageFactory setSize(int size) {
        this.size = size;
        return this;
    }

    @Override
    public Storage generate(String id) throws StorageException {
        return new DummyStorage(id, size);
    }
}
