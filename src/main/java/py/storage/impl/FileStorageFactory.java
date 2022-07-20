package py.storage.impl;

import java.io.File;

import py.storage.StorageFactory;

public abstract class FileStorageFactory implements StorageFactory {
    protected File file;

    public StorageFactory setFile(File file) {
        this.file = file;
        return this;
    }
}
