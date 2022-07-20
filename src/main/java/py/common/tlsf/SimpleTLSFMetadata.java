package py.common.tlsf;

/**
 * 
 * @author zjm
 *
 */
public class SimpleTLSFMetadata implements TLSFMetadata {
    private long firstLevelBitmap = 0l;

    private long[] secondLevelBitmap = new long[TLSFSpaceManager.FIRST_LEVEL_INDEX_COUNT];

    private long[][] segregatedList = new long[TLSFSpaceManager.FIRST_LEVEL_INDEX_COUNT][TLSFSpaceManager.SECOND_LEVEL_INDEX_COUNT];

    public SimpleTLSFMetadata() {
        for (int i = 0; i < TLSFSpaceManager.FIRST_LEVEL_INDEX_COUNT; i++) {
            for (int j = 0; j < TLSFSpaceManager.SECOND_LEVEL_INDEX_COUNT; j++) {
                segregatedList[i][j] = TLSFSpaceManager.NULL_SPACE_ADDRESS;
            }
        }
    }

    @Override
    public long getFirstLevelBitmap() {
        return firstLevelBitmap;
    }

    @Override
    public void setFirstLevelBitmap(long bitmap) {
        this.firstLevelBitmap = bitmap;
    }

    @Override
    public long getSecondLevelBitmap(int firstLevelIndex) {
        return secondLevelBitmap[firstLevelIndex];
    }

    @Override
    public void setSecondLevelBitmap(int firstLevelIndex, long bitmap) {
        secondLevelBitmap[firstLevelIndex] = bitmap;
    }

    @Override
    public long getItemFromSegregatedList(int firstLevelIndex, int secondLevelIndex) {
        return segregatedList[firstLevelIndex][secondLevelIndex];
    }

    @Override
    public void setItemToSegregatedList(int firstLevelIndex, int secondLevelIndex, long address) {
        segregatedList[firstLevelIndex][secondLevelIndex] = address;
    }
}
