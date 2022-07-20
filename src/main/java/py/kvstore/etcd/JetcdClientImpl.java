package py.kvstore.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.GetOption.SortOrder;
import io.etcd.jetcd.options.GetOption.SortTarget;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import py.kvstore.KvStoreException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static py.kvstore.etcd.Constants.*;

public class JetcdClientImpl implements JetcdClient {
    private Client client;
    private KV kvClient;
    private Lease leaseClient;
    private Lock lockClient;

    private String[] endpoints;

    JetcdClientImpl(String[] endpoints) {
        this.endpoints = endpoints;
        init();
    }

    private void init() {
        client = Client.builder().endpoints(endpoints).build();
        kvClient = client.getKVClient();
        lockClient = client.getLockClient();
        leaseClient = client.getLeaseClient();
    }

    private <T> T futureGet(CompletableFuture<T> future) throws KvStoreException {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new KvStoreException(e);
        }
    }

    @Override
    public void put(ByteSequence key, ByteSequence value, long leaseId) throws KvStoreException {
        futureGet(kvClient.put(key, value, PutOption.newBuilder().withLeaseId(leaseId).build()));
    }

    @Override
    public long put(ByteSequence key, ByteSequence value, int ttl) throws KvStoreException {
        long leaseId = grandLease(ttl);
        put(key, value, leaseId);
        return leaseId;
    }

    @Override
    public ByteSequence get(ByteSequence key) throws KvStoreException {
        GetResponse getRes = futureGet(kvClient.get(key));
        return getRes.getCount() == 0 ? null : getRes.getKvs().get(0).getValue();
    }

    @Override
    public List<KeyValue> getByPrefix(ByteSequence keyPrefix) throws KvStoreException {
        GetOption option = GetOption.newBuilder()
                .withPrefix(keyPrefix)
                .withSortOrder(SortOrder.ASCEND)
                .withSortField(SortTarget.KEY) //order by create time
                .build();
        return futureGet(kvClient.get(keyPrefix, option)).getKvs();
    }

    @Override
    public boolean remove(ByteSequence key) throws KvStoreException {
        futureGet(kvClient.delete(key));
        return true;
    }

    @Override
    public void removeByPrefix(ByteSequence keyPrefix) throws KvStoreException {
        DeleteOption deleteOption = DeleteOption.newBuilder().withPrefix(keyPrefix).build();
        futureGet(kvClient.delete(keyPrefix,deleteOption)).getDeleted();
    }

    @Override
    public boolean exist(ByteSequence key) throws KvStoreException {
        return get(key) != null;
    }

    @Override
    public void unlock(ByteSequence mutex) throws KvStoreException {
        futureGet(lockClient.unlock(mutex));
    }

    @Override
    public ByteSequence lock(ByteSequence mutex) throws KvStoreException {
        return futureGet(lockClient.lock(mutex, 0)).getKey();
    }

    @Override
    public ByteSequence tryLock(ByteSequence mutex, long timeout, TimeUnit timeUnit) {
        try {
            return lockClient.lock(mutex, 0).get(timeout, timeUnit).getKey();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ByteSequence putIfAbsent(ByteSequence keyBs, ByteSequence valueBs, long leaseId) throws KvStoreException {
        Txn txn = kvClient.txn();
        PutOption option = leaseId == NO_LEASE_ID ? PutOption.DEFAULT :
                PutOption.newBuilder().withLeaseId(leaseId).build();
        Cmp cmp = new Cmp(keyBs, Cmp.Op.EQUAL, CmpTarget.createRevision(0));
        CompletableFuture<io.etcd.jetcd.kv.TxnResponse> txnResp = txn.If(cmp)
                .Then(Op.put(keyBs, valueBs, option))
                .Else(Op.get(keyBs, GetOption.DEFAULT))
                .commit();
        List<GetResponse> getResponses = futureGet(txnResp).getGetResponses();
        return getResponses.size() == 0 ? null : getResponses.get(0).getKvs().size() == 0 ? null :
                getResponses.get(0).getKvs().get(0).getValue();

    }

    @Override
    public boolean replace(ByteSequence keyBS, ByteSequence expectBS, ByteSequence valueBS, long leaseId) throws KvStoreException {

        if (expectBS == null) {
            return Objects.equals(putIfAbsent(keyBS, valueBS, leaseId), expectBS);
        } else {
            Txn txn = kvClient.txn();
            PutOption option = leaseId == NO_LEASE_ID ? PutOption.DEFAULT :
                    PutOption.newBuilder().withLeaseId(leaseId).build();
            Cmp cmp = new Cmp(keyBS, Cmp.Op.EQUAL, CmpTarget.value(expectBS));
            CompletableFuture<io.etcd.jetcd.kv.TxnResponse> txnResp = txn.If(cmp)
                    .Then(Op.put(keyBS, valueBS, option))
                    .Else(Op.get(keyBS, GetOption.DEFAULT))
                    .commit();
            return futureGet(txnResp).getGetResponses().size() == 0;
        }
    }

    @Override
    public long incrementAndGet(ByteSequence key) throws KvStoreException {
        ByteBuffer buffer;
        ByteSequence lockKey = lock(key);
        ByteSequence val = get(key);
        long newVal = 0;
        if (val == null) {
            buffer = ByteBuffer.allocate(8);
            newVal = 1;
        } else {
            buffer = ByteBuffer.wrap(toBytes(val));
            long oldVal = buffer.getLong();
            newVal = oldVal + 1;
        }
        buffer.position(0);
        buffer.putLong(newVal);
        put(key, fromBytes(buffer.array()), NO_LEASE_ID);
        unlock(lockKey);
        return newVal;
    }

    @Override
    public long grandLease(long ttl) throws KvStoreException {
        return futureGet(leaseClient.grant(ttl)).getID();
    }

    @Override
    public long timeToLive(long leaseId) throws KvStoreException {
        return futureGet(leaseClient.timeToLive(leaseId, LeaseOption.DEFAULT)).getTTl();
    }

    @Override
    public long keepAliveOnce(long leaseId) throws KvStoreException {
        return futureGet(leaseClient.keepAliveOnce(leaseId)).getTTL();
    }

    @Override
    public AutoCloseable keepAlive(long leaseId) {
        // if you want observer see the following code
//        StreamObserver<LeaseKeepAliveResponse> observer = Observers.observer(response -> {
//            System.out.println(Thread.currentThread().getName() + ": " + response.getTTL());
//        });
//        try (CloseableClient c = leaseClient.keepAlive(leaseId, observer)) {
//            return c;
//        }
        return leaseClient.keepAlive(leaseId, null);
    }

    @Override
    public Txn txn() {
        return kvClient.txn();
    }

    @Override
    public void commitTxn(Txn txn) throws KvStoreException {
        futureGet(txn.commit());
    }

    @Deprecated
    private ByteSequence putIfAbsent2(ByteSequence keyBs, ByteSequence valueBs, long leaseId) throws KvStoreException {
        ByteSequence exist = get(keyBs);
        ByteSequence lockKey = null;
        if (exist == null) {
            lockKey = lock(keyBs);
            try {
                exist = get(keyBs);
                if (exist == null) {
                    put(keyBs, valueBs, leaseId);
                }
            } finally {
                if (lockKey != null) {
                    unlock(lockKey);
                }
            }
        }
        return exist;
    }
}
