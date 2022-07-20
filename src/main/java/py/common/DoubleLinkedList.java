package py.common;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of double linked list
 * 
 *  @author chenlia
 */
public class DoubleLinkedList<K> {
    private static final Logger logger = LoggerFactory.getLogger(DoubleLinkedList.class);

    private final ListNode<K> head;
    private int size = 0;

    public final static class ListNode<K> {
        private K content;
        private ListNode<K> prev;
        private ListNode<K> succ;
        private boolean isHead;

        public ListNode(K k, boolean head){
            content = k;
            prev = this;
            succ = this;
            isHead = head;
        }
        
        public ListNode(K k){
            this(k, false);
        }
        
        public ListNode(ListNode<K> pred, K k, ListNode<K> succ) {
            content = k;
            this.prev = pred;
            this.succ = succ;
            this.isHead = false;
        }
        
        void clear() {
            content = null;
            prev = null;
            succ = null;
        }
        
        public K content() {
            return content;
        }
        
        public ListNode<K> getPrevious() {
            return prev;
        } 
        
        public ListNode<K> getNext() {
            return succ;
        }
        
        public boolean hasNext() {
            return !succ.isHead;
        }
        
        public boolean hasPrevious() {
            return !prev.isHead;
        }
        
        public boolean isHead() {
            return isHead;
        }
        
        private void setPrevious(ListNode<K> pNode) {
            prev = pNode;
        }
        
        private void setNext(ListNode<K> pNode) {
            succ = pNode;
        }
        
        public boolean removeMyselfFromList() {
            if (prev != null && succ != null && !isHead) {
                // it is not head and has prev and succ
                prev.setNext(succ);
                succ.setPrevious(prev);
                clear();
                return true;
            } else {
                return false;
            }
        }
    }

    /** 
     * Iterator (cursor) for the double linked list. You can specify 
     * additional two ends so that the iteration can not pass the two ends.  
     * @author chenlia
     *
     * @param <K>
     */
    public final static class DoubleLinkedListIterator<K> {
        private ListNode<K> rightEnd;
        private ListNode<K> leftEnd;
        private ListNode<K> curPosition;

        DoubleLinkedListIterator(ListNode<K>curPosition, ListNode<K> left, ListNode<K> right){
            this.curPosition = curPosition;
            if (curPosition == null) {
                try {
                    throw new RuntimeException();
                } catch(Exception e) {
                    logger.error("curPosition is null", e);
                }
            }
            this.rightEnd = right;
            this.leftEnd = left;
        }

        public DoubleLinkedListIterator(ListNode<K> pos) {
            this(pos, null, null);
        }

        // Copy constructor
        public DoubleLinkedListIterator(DoubleLinkedListIterator<K> iterator) {
            this(iterator.curPosition, iterator.leftEnd, iterator.rightEnd);
        }
        
        // Copy constructor
        public DoubleLinkedListIterator(DoubleLinkedListIterator<K> iterator, ListNode<K> left, ListNode<K> right) {
            this(iterator.curPosition, left, right);
        }

        /*
         * move to next position
         */
        public DoubleLinkedListIterator<K> next() {
                curPosition = curPosition.succ;
                if (curPosition == null) {
                    try {
                        throw new RuntimeException();
                    } catch(Exception e) {
                        logger.error("curPosition is null", e);
                    }
                }
            return this;
        }

        /*
         * move to previous position
         */
        public DoubleLinkedListIterator<K> previous() {
                curPosition = curPosition.prev;
                if (curPosition == null) {
                    try {
                        throw new RuntimeException();
                    } catch(Exception e) {
                        logger.error("curPosition is null", e);
                    }
                }
            return this;
        }
        
        // if forward is true, then move from left to right
        public DoubleLinkedListIterator<K> move(boolean forward) {
            if (forward) {
                return next();
            } else {
                return previous();
            }
        }
        
        public K content() {
            return curPosition.content;
        }
        
        public ListNode<K> getCurrentPosition() {
            return curPosition;
        }
        
        public boolean hasNext() {
            if (!curPosition.hasNext()) {
                return false;
            } else if (rightEnd != null && curPosition == rightEnd) {
                // compare the object REFERENCE of my content with last
                // if there are equal, no more next then
                return false;
            } else {
                return true;
            }
        }
        
        public boolean hasPrevious() {
            if (!curPosition.hasPrevious()) {
                return false;
            } else if (leftEnd != null && curPosition == leftEnd) {
                return false;
            } else {
                return true;
            }
         }

        // if forward is true, then check the neighbour at the right (or next)
        public boolean hasNeighbour(boolean forward) {
            if (forward) {
                return hasNext();
            } else {
                return hasPrevious();
            }
        }
        
        public boolean pointToHead() {
            return curPosition.isHead;
        }
    }
    
    
    public DoubleLinkedList(K k) {
       head = new ListNode(k, true);
    }
    
    public DoubleLinkedList() {
        head = new ListNode(null, true);
    }
    
    // Sometime we need put some metadata to the head
    public K getHead() {
        return head.content;
    }
    
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
    
    /*
     * Return an iterator positioned at the tail.
     */
    public final DoubleLinkedListIterator<K> tail() {
        return new DoubleLinkedListIterator<K>(head.prev);
    }
    
    /*
     * Return an iterator positioned at the head.
     */
     public final DoubleLinkedListIterator<K> head() {
       return new DoubleLinkedListIterator<K>(head);
     }
     
    public ListNode<K> append(K k) {
        ListNode<K> node = new ListNode<K>(k);
        insertTail(node);
        return node;
    }
    
    /*
     * insert element after current position
     */
     public final ListNode<K>  insertAfter(K k, DoubleLinkedListIterator<K> cursor) {
       ListNode<K> newNode = new ListNode<K>(cursor.curPosition, k, cursor.curPosition.succ);
       newNode.succ.prev = newNode;
       cursor.curPosition.succ = newNode;
       size ++;
       return newNode;
     }

    /*
     * insert element before current position
     */
     public final ListNode<K> insertBefore(K k, DoubleLinkedListIterator<K> cursor) {
       ListNode<K> newNode = new ListNode<K>(cursor.curPosition.prev, k, cursor.curPosition);
       newNode.prev.succ = newNode;
       cursor.curPosition.prev = newNode;
       size ++;
       return newNode;
     }

    private void insertHead( ListNode<K> entry ) {
        Validate.notNull(head);
        entry.prev = head;
        entry.succ = head.succ;
        head.succ = entry;
        entry.succ.prev = entry;
        size ++;
    }

    private void insertTail( ListNode<K> entry ) {
        // get the tail first
        ListNode<K> tail = head.prev;
        Validate.notNull(tail);
        entry.prev = tail;
        entry.succ = tail.succ;
        tail.succ = entry;
        head.prev = entry;
        size ++;
    }

    // remove the first node in the list, (Not the head) and return the content of the node
    public  K removeFromHead() {
        if( size == 0) {
            Validate.isTrue(head.succ == head);
            Validate.isTrue(head.prev == head);
            return null;
        }

        ListNode<K> firstNode = head.succ;
        ListNode<K> secondNode = firstNode.succ;
        head.succ = secondNode;
        secondNode.prev = head;
        K content = firstNode.content();
        firstNode.clear();
        size --;
        return content;
    }

    // remove the first node in the list. (Not the head) and return the content of the node
    public K removeFromTail() {
        if( size == 0) {
            Validate.isTrue(head.succ == head);
            Validate.isTrue(head.prev == head);
            return null;
        }

        ListNode<K> tail = head.prev;
        ListNode<K> priorToTail = tail.prev;

        head.prev = priorToTail;
        priorToTail.succ = head;
        K content = tail.content();
        tail.clear();
        size --;
        return content;
    }
    
    //TODO: This function remove a node from the list. It doesn't check whether 
    // the node is in the list. In future, we need to add a map in this class 
    // to quickly find the node. Otherwise, we have to go through the whole list to 
    // find the node position. 
    // The reason we add this function instead of using ListNode.removeMyselfFromList()
    // is the size of the list has to be changed accordingly.
    public void removeNode(ListNode<K> k) {
        if (k.removeMyselfFromList()) {
            size --;
        }
    }

}
