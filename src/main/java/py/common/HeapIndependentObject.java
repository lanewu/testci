package py.common;

/**
 * This class is used by MutationLogFactory to manage mutation logs and reserved data buffers. Please use this interface
 * with caution.
 * 
 * It were preferable to use protected modifier to restrict its access, but it is not allow to do so for an interface.
 * Lame:(
 * 
 * @author chenlia
 *
 */
public class HeapIndependentObject {
    boolean free = false;

    /**
     * Free this object
     */
    public void free() {
        free = true;
    }

    /**
     * Allocate this object
     */
    public void allocate() {
        free = false;
    }

    /**
     * check if the object has been allocated
     * 
     * @return true the object has been allocated. False the object has been freed
     */
    public boolean isAllocated() {
        return !free;
    }

    public void setPersisted() {

    }
}
