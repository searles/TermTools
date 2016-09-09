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

    /**
     * Normalizes a term t using the term function fn. The reducts are stored inside the same termlist as t and the
     * link-fields are set to reducts. link does not represent the 1-step reduction. Normalforms are recognized by
     * a link field that points onto themselves. If a loop occurs, an exception is thrown (fn's may
     * use different means to deal with such to recover from such mistakes). Terms that were not rewritten
     * (either because they are in a subterm such that it was not necessary or because they ended in a loop)
     * point to null.
     * @param fn
     * @param t
     * @return
     */
    public static Term normalize(TermFn fn, Term t) {
        // if link in u is already set, it must have already been rewritten.
        t = resolveLinks(t);

        // rewrite it at root as long as possible.
        while (!t.normalform) {
            // normalize was not called yet on this term.
            Term u = fn.apply(t, t.parent);

            if(u == null) {
                // t cannot be reduced
                boolean subtermRewritten = false;

                for (Term subterm : t.args()) {
                    normalize2(fn, subterm);
                    if(subterm.link != null) subtermRewritten = true;
                }

                if (subtermRewritten) {
                    // some subterm was rewritten/normalized.
                    u = t.parent.insert(t);
                } else {
                    // no, I can't rewrite any subterm, so we are done.
                    // t is a normalform
                    t.normalform = true;
                    return t;
                }
            }
            // u contains a reduct of t.

            // if link in u is already set, it must have already been rewritten.
            u = resolveLinks(u);

            // connect it now that we are sure that no loops would be introduced.
            t = t.link = u;

            // if u (now t) is already a normalform, the loop will terminate.
        }

        return t;
    }

    public static Term normalize2(TermFn fn, Term t) {
        // if link in u is already set, it must have already been rewritten.
        t = resolveLinks(t);

        // rewrite it at root as long as possible.
        while (!t.normalform) {
            Term u = null;

            boolean subtermRewritten = false;

            for (Term subterm : t.args()) {
                normalize(fn, subterm);
                if(subterm.link != null) subtermRewritten = true;
            }

            if (subtermRewritten) {
                // some subterm was rewritten/normalized.
                u = t.parent.insert(t);
            } else {
                u = fn.apply(t, t.parent);

                if(u == null) {
                    t.normalform = true;
                    return t;
                }
            }
            // normalize was not called yet on this term.

            // u contains a reduct of t.

            // if link in u is already set, it must have already been rewritten.
            u = resolveLinks(u);

            // connect it now that we are sure that no loops would be introduced.
            t = t.link = u;

            // if u (now t) is already a normalform, the loop will terminate.
        }

        return t;
    }

    // what would be nice: A normalizing strategy that uses the termlist
    // because it would not require such deep recursion.
    // go from left to right. If a term is rewritten (it is innermost in that case)
    // its reducts should be inserted immediately afterwards.
}
