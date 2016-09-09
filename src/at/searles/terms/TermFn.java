package at.searles.terms;

import java.util.LinkedList;
import java.util.List;

/**
 * Functions for terms
 */
public interface TermFn {

    Term apply(Term t, TermList target);

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
        CycleException ex = null;

        // rewrite it at root as long as possible.
        while (!t.normalform) {
            // normalize was not called yet on this term.
            Term u = null;

            try {
                u = fn.apply(t, t.parent);
            } catch(CycleException c) {
                ex = c;
                // if there was an exception we might be more successful in the subterms
            }

            if (u == null) {
                // t cannot be rewritten at root. Can I normalize args?
                List<Term> args = new LinkedList<>();

                boolean subtermRewritten = false;

                for (Term subterm : t.args()) {
                    // the subterm might have been reduced earlier
                    Term original = subterm;

                    while(subterm.link != null) subterm = subterm.link;

                    t.link = subterm; // to avoid loops. Similar strategy inside of conditionalrule for conditions.

                    Term reducedSubterm = subterm;

                    // recursion into args
                    try {
                        reducedSubterm = normalize(fn, subterm); // here might be an exception.
                    } finally {
                        // unset t.link (it was set to the subterm)
                        t.link = null;
                        if (reducedSubterm != original) subtermRewritten = true;
                        args.add(reducedSubterm);
                    }
                }

                if (subtermRewritten) {
                    // some subterm was rewritten/normalized.
                    // observe that now for all arg in args, arg.normalform must be set.
                    u = t.copy(t.parent, args);
                } else {
                    // no, I can't rewrite any subterm, so we are done. t is a normalform or there was a cycle before.
                    if(ex != null) throw ex;
                    else {
                        t.normalform = true;
                        return t;
                    }
                }
            }

            // u contains a reduct of t.

            // if link in u is already set, it must have already been rewritten.
            // find the last one in the chain.
            while (u.link != null) {
                // all possible loops due to the condition or subterm condition
                // were unlinked, thus there should not be loops here.
                u = u.link;
            }

            // go to the next term.
            t = t.link = u;

            // if t was already a normalform, the loop will terminate.
        }

        return t;
    }
}
