package at.searles.terms;

import java.util.*;

/**
 * Functions for terms
 */
@FunctionalInterface
public interface TermFn {

    Term apply(Term t, TermList target);

    default Term transitive(Term t, TermList target) {
        Term u = this.apply(t, target);

        if(u == null) return null; // not applicable.

        // otherwise, apply as much as possible. Store the last reduct in t.
        while(u != null) {
            t = u;
            u = this.apply(t, target);
        }

        return t;
    }

    default Term subtermTree(Term t, TermList target) {
        // outer first
        Term u = this.apply(t, target);
        if(u != null) {
            return u;
        } else if(t.arity() == 0) {
            return null;
        } else {
            // do subterms
            ArrayList<Term> args = null;

            boolean isReduced = false;

            for (int i = 0; i < t.arity(); ++i) {
                Term arg = subtermTree(t.arg(i), target);

                if(isReduced) {
                    args.add(arg != null ? arg : t.arg(i));
                } else if(arg != null) {
                    isReduced = true;

                    // must initialize args.
                    args = new ArrayList<>(t.arity());

                    for(int k = 0; k < i; ++k) {
                        args.add(t.arg(k));
                    }

                    args.add(arg);
                }
            }

            return isReduced ? t.copy(target, args) : null;
        }
    }

    default Term subtermDag(TermFn fn, Term t, TermList target) {
        TreeMap<Term, Term> cache = new TreeMap<>(TermList.CMP);

        TreeSet<Term> queue = new TreeSet<>(TermList.CMP);
        TreeSet<Term> pending = new TreeSet<>(TermList.CMP);

        queue.add(t);

        while(!queue.isEmpty()) {
            Term u = queue.pollLast(); // fetch largest term (it would not be reused).

            assert !cache.containsKey(u);

            Term v = fn.apply(u, target);

            if(v != null) {
                // it was mapped
                cache.put(u, v);
            } else {
                // do on all subterms.
                pending.add(u);

                for(int i = 0; i < u.arity(); ++i) {
                    queue.add(u.arg(i));
                    // since I only fetch the largest, all subterms will be only added once,
                    // thus preserving the dag property.
                }
            }
        }

        ArrayList<Term> args = null; // will be created if necessary.

        // pending contains only terms that were not mapped.
        while(!pending.isEmpty()) {
            Term w = pending.pollFirst(); // fetch the one term that definitely has no subterm in there.

            // check whether a subterm was mapped.
            boolean isReduced = false;

            for(int i = 0; i < w.arity(); ++i) {
                Term arg = cache.get(w.arg(i));

                if(isReduced) {
                    // args was already initialized.
                    args.add(arg != null ? arg : w.arg(i));
                } else if (arg != null) {
                    // we have mapped subterms, thus do something about it.
                    isReduced = true;

                    // initialize args
                    if(args == null) {
                        args = new ArrayList<>(w.arity());
                    } else {
                        args.clear();
                        args.ensureCapacity(w.arity());
                    }

                    // and add former (unmapped) arguments.
                    for(int k = 0; k < i; ++k) {
                        args.add(w.arg(k));
                    }

                    args.add(arg);
                }
            }

            // and if so, then create a copy and put it into the cache.
            if(isReduced) {
                cache.put(w, w.copy(target, args));
            }
        }

        return cache.get(t); // returns null if not in cache.
    }



    // what would be nice: A normalizing strategy that uses the termlist
    // because it would not require such deep recursion.
    // go from left to right. If a term is rewritten (it is innermost in that case)
    // its reducts should be inserted immediately afterwards.
}
