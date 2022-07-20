package py.monitor.customizable.repository;

import py.monitor.exception.EmptyStoreException;

public interface AttributeStoreFactory {

    public AttributeMetadataStore create(Class<? extends AttributeMetadataStore> clazz) throws EmptyStoreException, Exception;
}
