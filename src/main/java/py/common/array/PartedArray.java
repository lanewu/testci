package py.common.array;

/**
 * <p>Depart a large array into small arrays, the reason to do this is this :
 *
 * <p><a href="https://bugs.openjdk.java.net/browse/JDK-8057003">Large reference arrays cause extremely long synchronization times</a>
 *
 * <p> We won't need this class if migrating to Java 9, cause the above bug was fixed only after Java 9
 *
 * @param <T>
 */
@SuppressWarnings("unchecked") public class PartedArray<T> {

    static int PART_THRESHOLD = 1024 * 1024;

    private final int size;
    private final int partedArrayCount;
    private final int partedArraySize;
    private final Object[][] arrays;

    public PartedArray(int size) {
        this(size, size > PART_THRESHOLD ? (int) Math.sqrt(size) : size);
    }

    public PartedArray(int size, int maxSizeEach) {
        this.size = size;
        this.partedArrayCount = size / maxSizeEach + (size % maxSizeEach == 0 ? 0 : 1);
        this.partedArraySize = size > maxSizeEach ? maxSizeEach : size;
        this.arrays = new Object[partedArrayCount][partedArraySize];
    }

    public int size() {
        return size;
    }

    public void set(int index, T val) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        arrays[index / partedArraySize][index % partedArraySize] = val;
    }

    public T get(int index) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (T) arrays[index / partedArraySize][index % partedArraySize];
    }

}
