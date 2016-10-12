package at.searles.terms;

import java.util.LinkedList;
import java.util.List;

/**
 * Functions for terms
 */
public interface TermFn {

    Term apply(Term t, TermList target);

    static Term resolveLinks(Term t) {
        // if link in u is already set, it must have already been rewritten.
        if(t.link != null) {
            while(t.link.link != null) t.link = t.link.link;

            t = t.link;

            // if it isn't a normalform, then sry, a loop.
            if(!t.normalform) throw new CycleException(t);
        }

        return t;
    }



    // what would be nice: A normalizing strategy that uses the termlist
    // because it would not require such deep recursion.
    // go from left to right. If a term is rewritten (it is innermost in that case)
    // its reducts should be inserted immediately afterwards.
}
