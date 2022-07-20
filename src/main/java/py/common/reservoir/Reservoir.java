package py.common.reservoir;

public interface Reservoir<T> {
    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    int size();

    /**
     * Adds a new recorded value to the reservoir.
     *
     * @param value a new recorded value
     */
    void update(T value);

    T[] getSnapshot();
}
