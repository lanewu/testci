package py.kvstore;

import java.util.List;
import py.common.struct.Pair;
import py.kvstore.transaction.Transaction;
import static py.kvstore.KvConstants.*;
import java.util.Map;

public interface KvStore {
//todo add apidoc for all method
    /****************** map function ***********************/

    default void put(byte[] key, byte[] value) throws KvStoreException {
        put(key, value, NO_LEASE);
    }

    default void putStr(String key, String value) throws KvStoreException {
        put(key.getBytes(), value.getBytes());
    }

    default void putInt(String key, int value) throws KvStoreException {
        put(key.getBytes(), int2bytes(value));
    }

    default void putInt(String key, long value) throws KvStoreException {
        put(key.getBytes(), long2bytes(value));
    }

    /**
     * the value put will be combined to a lease of the leaseId
     * if the lease already expired, an exception will be thrown
     * @param key
     * @param value
     * @param leaseId
     * @throws KvStoreException
     */
    void put(byte[] key, byte[] value, long leaseId) throws KvStoreException;

    /**
     * the value put will be expired in ttl seconds
     * @param key
     * @param value
     * @param ttl time to live
     * @return the leaseId associated with the given ttl
     * @throws KvStoreException
     */
    long put(byte[] key, byte[] value, int ttl) throws KvStoreException;

    byte[] get(byte[] key) throws KvStoreException;

    default String getStr(String key) throws KvStoreException {
        return bytes2str(get(key.getBytes()));
    }

    default Integer getInt(byte[] key) throws KvStoreException {
        return bytes2int(get(key));
    }

    default Integer getInt(String key) throws KvStoreException {
        return bytes2int(get(key.getBytes()));
    }

    default Long getLong(String key) throws KvStoreException {
        return bytes2long(get(key.getBytes()));
    }

    List<Pair<byte[], byte[]>> getByPrefix(byte[] keyPrefix) throws KvStoreException;

    boolean remove(byte[] key) throws KvStoreException;

    void removeByPrefix(byte[] keyPrefix) throws KvStoreException;

    boolean exist(byte[] key) throws KvStoreException;

    default byte[] putIfAbsent(byte[] key, byte[] value) throws KvStoreException {
        return putIfAbsent(key, value, NO_LEASE);
    }

    default String putIfAbsent(String key, String value, long leaseId) throws KvStoreException {
        byte[] bytes = putIfAbsent(key.getBytes(), value.getBytes(), leaseId);
        return bytes == null ? null : bytes2str(bytes);
    }

    byte[] putIfAbsent(byte[] key, byte[] value, long leaseId) throws KvStoreException;

    default boolean replace(byte[] key, byte[] expect, byte[] value) throws KvStoreException {
        return replace(key, expect, value, NO_LEASE);
    }

    default boolean replace(String key, String expect, String value, long leaseId) throws KvStoreException {
        return replace(key.getBytes(), expect.getBytes(), value.getBytes(), leaseId);
    }

    boolean replace(byte[] key, byte[] expect, byte[] value, long leaseId) throws KvStoreException;

    /****************** lease function ***********************/
    /**
     * lease
     * @param ttl
     * @return
     * @throws Exception
     */
    long grandLease(long ttl) throws KvStoreException;

    long timeToLive(long leaseId) throws KvStoreException;

    long keepAliveOnce(long leaseId) throws KvStoreException;

    AutoCloseable keepAlive(long leaseId);

    /****************** transaction function ***********************/

    Transaction beginTxn();

    void commitTxn(Transaction txn) throws KvStoreException;

    /****************** lock function ***********************/
}
