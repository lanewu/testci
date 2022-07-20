package py.kvstore.transaction;

import py.kvstore.operation.Operation;

import java.util.ArrayList;
import java.util.List;

import static py.kvstore.KvConstants.*;

public class Transaction {
    protected List<Operation> operations = new ArrayList<>();

    public Transaction put(byte[] key, byte[] value) {
        return put(key, value, NO_LEASE);
    }

    public Transaction put(byte[] key, byte[] value, long leaseId) {
        operations.add(Operation.put(key, value, leaseId));
        return this;
    }

}
