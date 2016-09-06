package at.searles.terms;

import java.util.*;

/**
 * A data structure to contain nodes of terms. Technically a single linked list, this DS mainly
 * provides the insert-method that ensures that a term is always stored as a DAG with maximum
 * sharing. This property is essential for some algorithms in Term.
 */
public class TermList implements Iterable<Term> {
    private Node head = null;

    /**
     * Insert a term in which link is possibly set. This might require a relabeling of lambda variables.
     * @param t
     * @return
     */
    Node insertNode(Term t) {
        // first, insert all subterms of t.
        t.initLevel(new LinkedList<>());

        TermList.Node ret = t.insertInto(this);

        t.cleanUpInsert();

        return ret;
    }

    public Term insert(Term t) {
        return insertNode(t).value;
    }

    /**
     * Makes sure that t is inside this termlist. Since the algorithm of 'insert' inserts
     * the subterms first, the subterms in t must be identical to the ones of the found termms.
     * @param t
     * @param max Calling methods can determine a node of a subterm to speed up this method. If null, then searching
     *            will start from head.
     * @return
     */
    Node assertInList(Term t, TermList.Node max) {
        if(head == null) {
            // in this case, t better does not contain any subterms...
            return head = new Node(t);
        }

        // Searching for term starts behind max.
        Node prev = max;
        Node next = max == null ? head : max.next;

        while(next != null && !next.value.eq(t)) {
            prev = next;
            next = next.next;
        }

        if(next != null) {
            // found it.
            return next;
        } else {
            // did not find it.
            return new Node(t, prev); // hence insert it.
        }
    }

    @Override
    public Iterator<Term> iterator() {
        // The iterator is a bit tricky. I want to allow for live-updates, thus the last returned node is stored
        // (null in the beginning)
        return new Iterator<Term>() {

            Node ptr = null;

            @Override
            public boolean hasNext() {
                return (ptr == null && head != null) || (ptr != null && ptr.next != null);
            }

            @Override
            public Term next() {
                ptr = ptr == null ? head : ptr.next;
                return ptr.value;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Iterator<Term> it = iterator();

        if(it.hasNext()) {
            sb.append(it.next());
            while(it.hasNext()) {
                sb.append(" :: ").append(it.next());
            }
        }

        return sb.toString();
    }

    /**
     * Node in a term list. default visibility so that Term can access this class (required for the is-field).
     */
    public class Node {
        public final Term value;
        final int index;

        Node next = null;

        Node(Term t) {
            this.value = t;
            index = 0;
        }

        Node(Term t, Node last) {
            this.value = t;
            last.next = this;
            this.index = last.index + 1;
        }
    }

}
