package py.common.tlsf.bytebuffer.manager;

import py.common.tlsf.SimpleTLSFSpaceDivisionMetadata;
import py.common.tlsf.TLSFSpaceDivisionMetadata;

/**
 * An implementation for direct byte buffer manager of interface {@link TLSFSpaceDivisionMetadata}.
 * 
 * @author zjm
 *
 */
public class ByteBufferDivisionMetadata extends SimpleTLSFSpaceDivisionMetadata {

    @Override
    public boolean seperated() {
        // the metadata of tlsf division is seperated from data in space
        return true;
    }

    @Override
    public long getNextPhysicalAddress(long address) {
        DivisionMetadata metadata = metadataTable.get(address);
        long nextPhyAddr = metadata.curPhyAddr + getAccessibleMemSize(address);
        return nextPhyAddr;
    }
}
