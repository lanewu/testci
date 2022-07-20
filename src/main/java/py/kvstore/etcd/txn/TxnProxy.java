package py.kvstore.etcd.txn;

import io.etcd.jetcd.Txn;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import py.exception.NotSupportedException;
import py.kvstore.operation.Operation;
import static py.kvstore.operation.Operation.*;
import static py.kvstore.etcd.Constants.*;
import py.kvstore.transaction.Transaction;

public class TxnProxy extends Transaction{
    private final Txn txn;

    public TxnProxy(Txn txn) {
        this.txn = txn;
    }

    public Txn build() {
        Op[] ops = new Op[operations.size()];
        for (int i = 0; i < ops.length; i++) {
            Operation oper = operations.get(i);
            switch (oper.type()) {
                case PUT:
                    PutOperation putOper = (PutOperation) oper;
                    ops[i] = Op.put(fromBytes(putOper.key()), fromBytes(putOper.value()),
                            PutOption.newBuilder().withLeaseId(putOper.leaseId()).build());
                    break;
                default:
                    throw new NotSupportedException(oper.toString());
            }

        }
        return txn.Then(ops);
    }

}
