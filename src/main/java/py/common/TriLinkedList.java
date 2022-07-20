package py.common;

import org.apache.commons.lang3.Validate;

/**
 * An implementation of triple linked list. Horizontally, it is a double linked list, 
 * and vertically, it is a single linked list.
 * 
 *  @author chenlia
 */
public class TriLinkedList<K> {
    private ListNode<K> head = new ListNode(null);
    private int size = 0;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void insertHead( ListNode<K> entry ) {
        Validate.notNull(head);
        entry.hPred = head;
        entry.hSucc = head.hSucc;
        head.hSucc = entry;
        entry.hSucc.hPred = entry;
    }

    private void insertTail( ListNode<K> entry ) {
        // get the tail first
        ListNode<K> tail = head.hPred;
        Validate.notNull(tail);
        entry.hPred = tail;
        entry.hSucc = tail.hSucc;
        tail.hSucc = entry;
        head.hPred = entry;
    }

    // remove the first node in the list. (Not the head)
    private void removeFromHead() {
        if( size == 0) {
            Validate.isTrue(head.hSucc == head);
            Validate.isTrue(head.hPred == head);
            return;
        }

        ListNode<K> firstNode = head.hSucc;
        ListNode<K> secondNode = head.hSucc;
        head.hSucc = secondNode;
        secondNode.hPred = head;
        firstNode.clear();
    }

    // remove the first node in the list. (Not the head)
    private void removeFromTail() {
        if( size == 0) {
            Validate.isTrue(head.hSucc == head);
            Validate.isTrue(head.hPred == head);
            return;
        }

        ListNode<K> tail = head.hPred;
        ListNode<K> priorToTail = tail.hPred;

        head.hPred = priorToTail;
        priorToTail.hSucc = head;
        tail.clear();
    }

    public final static class ListNode<K> {
        K content;
        ListNode<K> hPred;
        ListNode<K> hSucc;
        ListNode<K> vNext;

        public ListNode(K k){
            content = k;
            hPred = this;
            hSucc = this;
            vNext = null;
        }

        void clear() {
            content = null;
            hPred = null;
            hSucc = null;
            vNext = null;
        }
    }

    public final static class ListIterator<K> {
        TriLinkedList<K> owner;
        ListNode<K> pos;

        ListIterator(TriLinkedList<K> owner,  ListNode<K>pos){
            this.owner = owner;
            this.pos = pos;
        }

        ListIterator(TriLinkedList<K> owner) {
            this.owner = owner;
            this.pos = owner.head;
        }

        /*
         * check whether object owns the iterator
         */
        public boolean belongsTo(Object owner) {
            return this.owner == owner;
        } 

        /*
         * move to next position
         */
        public void next() {
            pos = pos.hSucc;
        }

        /*
         * move to previous position
         */
        public void previous() {
            pos = pos.hPred;
        }
    }
}
