package py.monitor.customizable.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import py.monitor.exception.EmptyStoreException;
import py.monitor.jmx.server.ResourceType;

/**
 * 
 * @author sxl
 * 
 */
public interface AttributeMetadataStore {
    public void load() throws EmptyStoreException, Exception;

    public void add(AttributeMetadata attributeMetadata) throws Exception;

    public void append(Collection<AttributeMetadata> attributeMetadatas) throws Exception;

    public void delete(long attributeMetadataIndex) throws Exception;

    public void set(AttributeMetadata attributeMetadata) throws Exception;

    public boolean contains(AttributeMetadata attributeMetadata) throws Exception;

    public AttributeMetadata get(long attributeMetadataId) throws Exception;

    public void saveOrUpdate(AttributeMetadata attributeMetadata) throws Exception;

    public Set<AttributeMetadata> get(List<Long> attributeMetadataIds) throws Exception;

    public AttributeMetadata get(String attributeMetadataName) throws Exception;

    public Set<AttributeMetadata> getByServiceName(String serviceName) throws Exception;

    public Set<AttributeMetadata> getByResourceType(ResourceType resourceType) throws Exception;

    public Set<AttributeMetadata> getAll() throws Exception;

    public Set<AttributeMetadata> getAllByGroupName(String groupName) throws Exception;

    public Set<String> getServiceNameByAttributeId(long AttributeId) throws Exception;

    public long size();

    public void commit() throws Exception;
}
