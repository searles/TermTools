package at.searles.terms.rules;

import at.searles.terms.Term;
import at.searles.terms.TermList;

public interface Rule {
	/**
	 * Applies this rule to t, inserting a reduct into the termqueue in target.
	 * @param t the term to be rewritten
	 * @param target if null, then a new termqueue is created.
	 * @return null if not applicable
	 */
	Term apply(Term t, TermList target);
}
