package py.storage;

/**
 * Created by xcs on 18-5-16.
 */
public enum StorageIOType {
    /*
     * SYNCIO meas read and write.
     * LINUXAIO meas io_xxxxx.
     * SYNCAIO meas aio_read and aio_write.
     */
    SYNCIO, LINUXAIO, SYNCAIO;

}
