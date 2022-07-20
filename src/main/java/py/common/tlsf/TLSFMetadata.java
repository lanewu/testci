package py.common.tlsf;

/**
 * 
 * @author zjm
 *
 */
public interface TLSFMetadata {

    public long getFirstLevelBitmap();

    public void setFirstLevelBitmap(long bitmap);

    public long getSecondLevelBitmap(int firstLevelIndex);

    public void setSecondLevelBitmap(int firstLevelIndex, long bitmap);

    public long getItemFromSegregatedList(int firstLevelIndex, int secondLevelIndex);

    public void setItemToSegregatedList(int firstLevelIndex, int secondLevelIndex, long address);
}
