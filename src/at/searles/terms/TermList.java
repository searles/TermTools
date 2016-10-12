package at.searles.terms;

import java.util.*;

/**
 * A data structure to contain nodes of terms. Technically a single linked list, this DS mainly
 * provides the insert-method that ensures that a term is always stored as a DAG with maximum
 * sharing. This property is essential for some algorithms in Term.
 */
public class TermList /*implements Iterable<Term>*/ {

    // this comparator makes args smaller than superterms.
    public static final Comparator<? super Term> CMP = (a, b) -> {
        if(a.parent != b.parent) throw new IllegalArgumentException("compared terms must be in same termlist");

        return Integer.compare(a.index, b.index);
    };

    private Term head = null;

    public TermList() {}

    TermList(Term head) {
        // this one is here just for testing...
        this.head = head;
        this.head.parent = this;
    }

    /**
     * Insert a term in which link is possibly set. This might require a relabeling of lambda variables.
     * If link is not set and the subterms are already inserted, it is better to use Term.copy instead.
     * @param t
     * @return
     */
    public Term insert(Term t) {
        // first, insert all args of t.
        t.initLevel(new LinkedList<>());

        Term ret = t.insertInto(this);

        t.uninsert();

        return ret;
    }

    /**
     * Makes sure that t is inside this termlist. Since the algorithm of 'insert' inserts
     * the args first, the args in t must be identical to the ones of the found terms.
     * @param t t is a new term. Copies are not allowed because they are already in the list.
     * @param max Calling methods can determine a node of a subterm to speed up this method. If null, then searching
     *            will start from head.
     * @return Either the node in this termlist that is equivalent to
     */
    Term findOrAppend(Term t, Term max) {
        if(head == null) {
            if(t.index != -1) throw new IllegalArgumentException();
            // in this case, t better does not contain any args...
            t.parent = this;
            return head = t;
        }

        // Searching for term starts behind max.
        Term prev = max;
        Term next = max == null ? head : max.next;

        while(next != null && !next.eq(t)) {
            prev = next;
            next = next.next;
        }

        if(next != null) {
            // found it.
            return next;
        } else {
            // did not find it.
            prev.append(t);
            return t; // hence append and return it.
        }
    }

    /*@Override
    public Iterator<Term> iterator() {
        // The iterator is a bit tricky. I want to allow for live-updates, thus the last returned node is stored
        // (null in the beginning)
        return new Iterator<Term>() {

            Term ptr = null;

            @Override
            public boolean hasNext() {
                return (ptr == null && head != null) || (ptr != null && ptr.next != null);
            }

            @Override
            public Term next() {
                ptr = ptr == null ? head : ptr.next;
                return ptr;
            }
        };
    }*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Term t = head;

        if(t != null) {
            sb.append(t);

            for(t = t.next; t != null; t = t.next) {
                sb.append(" :: ").append(t);
            }
        }

        return sb.toString();
    }

    public int size() {
        int i = 0;
        for(Term t = head; t != null; t = t.next) i++;
        return i;
    }

    /**
     * Returns a map of all free variables with their links.
     * @return
     */
    public Map<Var, Term> matcher() {
        Map<Var, Term> map = new TreeMap<>((v1, v2) -> v1.id.compareTo(v2.id));

        for(Term t = head; t != null; t = t.next) {
            if(t instanceof Var && t.link != null) {
                map.put((Var) t, t.link);
            }
        }

        return map;
    }

    /**
     * Clears all links in this termlist.
     */
    public void clearMatcher() {
        for(Term t = head; t != null; t = t.next) {
            t.link = null;
        }
    }

    /**
     * Creates a copy of this termlist where also link-entries are transfered but not followed.
     * The order of terms in the resulting termlist will be the same as in this. Instances of Var that
     * are inside renaming are renamed to the corresponding entry.
     * Make sure that renaming does not rename to already existing variables (unless you want to).
     * @param termsOfInterest Terms whose correspondant should be put into termMapping.
     * @param termMapping
     * @param renaming The renaming. Again, make sure that all images are distinct from
     * @return
     */
    /*public TermList createCopy(TreeSet<Term> termsOfInterest, TreeMap<Term, Term> termMapping, Map<String, String> renaming) {
        TermList newList = new TermList();

        for(Term t : termsOfInterest) {
            termMapping.put(t, t.shallowInsertInto(newList, renaming));
        }

        return null;
    }*/

    /**
     * Node in a term list. default visibility so that Term can access this class (required for the is-field).
     */
    public static class Node implements Comparable<Node> {
        // this could be easily merged with Term but I think keeping them separated is better because
        // of modularity.

        int index = -1;
        Term next = null;

        public TermList parent = null;

        Node append(Term nextOne) {
            if(next != null) throw new IllegalArgumentException();
            if(nextOne.index != -1) throw new IllegalArgumentException();
            nextOne.index = index + 1;
            this.next = nextOne;
            nextOne.parent = parent;
            return nextOne;
        }

        @Override
        public int compareTo(Node other) {
            if(parent != other.parent) throw new IllegalArgumentException("compared nodes must be in same termlist");
            return Integer.compare(this.index, other.index);
        }
    }

}
