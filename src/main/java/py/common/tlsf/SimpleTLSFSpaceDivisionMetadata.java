package py.common.tlsf;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author zjm
 *
 */
public class SimpleTLSFSpaceDivisionMetadata implements TLSFSpaceDivisionMetadata {
    private static final Logger logger = LoggerFactory.getLogger(SimpleTLSFSpaceDivisionMetadata.class);

    protected static class DivisionMetadata {
        public long prePhyAddr = TLSFSpaceManager.NULL_SPACE_ADDRESS;
        public long curPhyAddr = TLSFSpaceManager.NULL_SPACE_ADDRESS;
        public long sizeField = 0l;
        public long nextFreeAddr = TLSFSpaceManager.NULL_SPACE_ADDRESS;
        public long preFreeAddr = TLSFSpaceManager.NULL_SPACE_ADDRESS;
    }

    protected final Map<Long, DivisionMetadata> metadataTable = new HashMap<>();
    
    @Override
    public boolean seperated() {
        return false;
    }

    @Override
    public long getPrePhysicalAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return metadata.prePhyAddr;
    }

    @Override
    public void setPrePhysicalAddress(long address, long prePhysicalAddress) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }

        metadata.prePhyAddr = prePhysicalAddress;
    }

    @Override
    public long getNextPhysicalAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        long nextPhyAddr = metadata.curPhyAddr + TLSFSpaceDivisionMetadata.ASSEMEM_OFFSET
                + getAccessibleMemSize(address) - TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES;
        return nextPhyAddr;
    }

    @Override
    public long getAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return metadata.curPhyAddr;
    }

    @Override
    public void setAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }

        metadata.curPhyAddr = address;
    }

    @Override
    public long getAccessibleMemSize(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return metadata.sizeField & ~(BUFFER_FREE_BIT | PRE_BUFFER_FREE_BIT);
    }

    @Override
    public void setAccessibleMemSize(long address, long accessibleMemSize) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.sizeField = accessibleMemSize | (metadata.sizeField & (BUFFER_FREE_BIT | PRE_BUFFER_FREE_BIT));
    }

    @Override
    public long getPreFreeAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return metadata.preFreeAddr;
    }

    @Override
    public void setPreFreeAddress(long address, long preFreeAddress) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.preFreeAddr = preFreeAddress;
    }

    @Override
    public long getNextFreeAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return metadata.nextFreeAddr;
    }

    @Override
    public void setNextFreeAddress(long address, long nextFreeAddress) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }

        metadata.nextFreeAddr = nextFreeAddress;
    }

    @Override
    public boolean isFree(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return (metadata.sizeField & BUFFER_FREE_BIT) != 0;
    }

    @Override
    public void setFree(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.sizeField |= BUFFER_FREE_BIT;
    }

    @Override
    public boolean isPreFree(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        return (metadata.sizeField & PRE_BUFFER_FREE_BIT) != 0;
    }

    @Override
    public void setPreFree(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.sizeField |= PRE_BUFFER_FREE_BIT;
    }

    @Override
    public void setUsed(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.sizeField &= ~BUFFER_FREE_BIT;
    }

    @Override
    public void setPreUsed(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        if (metadata == null) {
            metadata = new DivisionMetadata();
            metadataTable.put(address, metadata);
        }
        metadata.sizeField &= ~PRE_BUFFER_FREE_BIT;
    }
    
    public void clear(long address) {
        metadataTable.remove(address);
    }

}
