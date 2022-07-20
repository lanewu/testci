package py.kvstore.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Txn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;
import py.common.struct.Pair;
import py.exception.NotSupportedException;
import py.kvstore.KvStore;
import py.kvstore.KvStoreException;
import py.kvstore.etcd.txn.TxnProxy;
import py.kvstore.transaction.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static py.kvstore.etcd.Constants.*;

public interface JetcdClient extends KvStore {
//todo add apidoc for all method
    /**
     * put
     * @param key
     * @param value
     * @throws Exception
     */

    default void put(byte[] key, byte[] value, long leaseId) throws KvStoreException {
        put(fromBytes(key), fromBytes(value), leaseId);
    }

    default long put(byte[] key, byte[] value, int ttl) throws KvStoreException {
        return put(fromBytes(key), fromBytes(value), ttl);
    }

    default void put(ByteSequence key, ByteSequence value) throws KvStoreException {
        put(key, value, NO_LEASE_ID);
    }

    void put(ByteSequence key, ByteSequence value, long leaseId) throws KvStoreException;

    long put(ByteSequence key, ByteSequence value, int ttl) throws KvStoreException;

    /**
     * get
     * @param key
     * @return
     * @throws Exception
     */
    default byte[] get(byte[] key) throws KvStoreException {
        ByteSequence bs = get(fromBytes(key));
        return bs == null ? null : bs.getBytes();
    }

    ByteSequence get(ByteSequence key) throws KvStoreException;

    default List<Pair<byte[], byte[]>> getByPrefix(byte[] keyPrefix) throws KvStoreException {
        List<KeyValue> kvs = getByPrefix(fromBytes(keyPrefix));

        List<Pair<byte[], byte[]>> ret = new ArrayList<>(kvs.size());
        for (KeyValue kv : kvs) {
            ret.add(new Pair<>(kv.getKey().getBytes(), kv.getValue().getBytes()));
        }

        return ret;
    }

    List<KeyValue> getByPrefix(ByteSequence keyPrefix) throws KvStoreException;

    /**
     * delete
     * @param key
     * @throws Exception
     */

    default boolean remove(byte[] key) throws KvStoreException {
        return remove(fromBytes(key));
    }

    boolean remove(ByteSequence key) throws KvStoreException;

    default void removeByPrefix(byte[] keyPrefix) throws KvStoreException {
        removeByPrefix(fromBytes(keyPrefix));
    }
    void removeByPrefix(ByteSequence keyPrefix) throws KvStoreException;
    /**
     * exist
     * @param key
     * @return
     * @throws Exception
     */

    default boolean exist(byte[] key) throws KvStoreException {
        return exist(fromBytes(key));
    }

    boolean exist(ByteSequence key) throws KvStoreException;

    /**
     * lock unlock
     * @param mutex
     * @throws Exception
     */
    void unlock(ByteSequence mutex) throws KvStoreException;

    ByteSequence lock(ByteSequence mutex) throws KvStoreException;

    ByteSequence tryLock(ByteSequence mutex, long timeout, TimeUnit timeUnit);

    /**
     * put if absent
     * @param key
     * @param value
     * @return
     * @throws Exception
     */

    default byte[] putIfAbsent(byte[] key, byte[] value, long leaseId) throws KvStoreException {
        ByteSequence exist = putIfAbsent(fromBytes(key), fromBytes(value), leaseId);
        return exist == null ? null : toBytes(exist);
    }

    ByteSequence putIfAbsent(ByteSequence keyBs, ByteSequence valueBs, long leaseId) throws KvStoreException;

    /**
     * replace
     * @param key
     * @param expect
     * @param value
     * @return
     * @throws Exception
     */

    default boolean replace(byte[] key, byte[] expect, byte[] value, long leaseId) throws KvStoreException {
        return replace(fromBytes(key), fromBytes(expect), fromBytes(value), leaseId);
    }

    boolean replace(ByteSequence key, ByteSequence expect, ByteSequence value, long leaseId) throws KvStoreException;

    /**
     * increment and get
     * @param key
     * @return
     * @throws Exception
     */

    long incrementAndGet(ByteSequence key) throws KvStoreException;

    /**
     * build an txn, client who call this method should fill operations in within [PUT/DEL/TXN]
     * then call {@link #commitTxn(Transaction)}
     * @return
     */
    default Transaction beginTxn() {
        return new TxnProxy(txn());
    }

    Txn txn();

    default void commitTxn(Transaction txn) throws KvStoreException {
        if (!(txn instanceof TxnProxy))
            throw new KvStoreException(new NotSupportedException());
        TxnProxy txnProxy = (TxnProxy)txn;
        commitTxn(txnProxy.build());
    }

    void commitTxn(Txn txn) throws KvStoreException;
}
