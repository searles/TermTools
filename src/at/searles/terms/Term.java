package at.searles.terms;

import java.util.*;

public abstract class Term {

	public Term link = null; // used for substitutions

	protected final int level; // lambda level
	protected final int maxLambdaIndex; // max lambda index in a subterm, -1 if no lambda variable below.

	protected Term(int level, int maxLambdaIndex) {
		this.level = level;
		this.maxLambdaIndex = maxLambdaIndex;
	}

	public boolean closed() {
		return maxLambdaIndex < level;
	}

	@Override
	public String toString() {
		// Just a forward (so that various annotations can be added during testing...)
		return str();
	}

	/**
	 * Simple toString() without consideration of link/inserted etc...
	 * @return
     */
	protected abstract String str();

	// The following fields are used for the insert-algorithm

	/**
	 * When inserting this term into a List, this is-field stores the pointer onto the list
	 * node. If no list node has been assigned yet, it contains null.
	 */
	private TermList.Node inserted = null; // used by insertInto.

	protected int insertLevel = -1; // lambda level of the inserted term.

	/**
	 * For terms without links, checking whether a term has some bound variables that is not yet bound
	 * by a lambda can be done via a simple comparison of maxLambdaIndex and level (method closed()).
	 * If there are links, there may be clashes, therefore I use 'insertedClosed' instead.
	 * [the same lambda variable might have different insertions depending on other subterms]
	 */
	protected boolean insertedClosed = false;  // true if there are no unbound lambda variables.

	/**
	 * This method prepares the insertion. It sets the insertLevel and insertedClosed-flag.
	 * @param lvs lambda variables that are collected in subterms.
	 * @return
     */
	void initLevel(List<LambdaVar> lvs) {
		if (insertLevel == -1) {
			if (link != null) {
				link.initLevel(lvs);
				this.insertedClosed = link.insertedClosed;
				this.insertLevel = link.insertLevel;
			} else {
				auxInitLevel(lvs);
			}
		}
	}

	abstract void auxInitLevel(List<LambdaVar> lvs);

	abstract TermList.Node auxInsert(TermList list);

	/**
	 * Inserts the termqueue in 'this' into s. The is-field in 'this' is used to store already inserted nodes.
	 * It follows links in terms.
	 * @param list the list in which this is inserted. null causes unexpected behaviour.
	 * @return the inserted term.
	 */
	TermList.Node insertInto(TermList list) {
		// check whether inserted should be set.
		// Here, it is checked whether all elements in lvStack (ie all bound variables)
		// are set to identity.
		// This could be optimized by limiting this to all bound variables that are actually used in a subterm,
		// but such an optimization might be memory consuming.

		if(insertedClosed && inserted != null) {
			return inserted;
		} else if(link != null) {
			// resolve link.
			return inserted = link.insertInto(list);
		} else {
			return inserted = this.auxInsert(list);
		}
	}

	/**
	 * insertInto sets the inserted-field to equivalent terms in another termlist
	 * In order to unset the inserted-field, the tree-term-structure is traversed
	 * in this recursive function and all insert-fields are cleared (set to null), insertLevel is reset and so is "insertedClosed".
	 */
	void cleanUpInsert() {
		if(insertLevel != -1) {
			inserted = null;
			insertLevel = -1;
			insertedClosed = false;

			if(link != null) {
				link.cleanUpInsert();
			} else {
				for (Term subterm : subterms()) {
					subterm.cleanUpInsert();
				}
			}
		}
	}

	/**
	 * Returns an iterable of all subterms
	 * @return
	 */
	public abstract Iterable<Term> subterms(); // iterate through all subterms

	/**
	 * Checks whether this is equal to t where for the subterms
	 * identity is used. This method is tailored to properly work with the
	 * insert algorithm to search for terms that were created with the copy method.
	 * @param t
	 * @return
     */
	abstract boolean eq(Term t);

	/**
	 * This field is used to replace terms by other terms. If an algorithm (eg unification) can give rise to
	 * cycles, use TODO to check for cycles!
	 *
	Term link = null;

	public void mark(int value) {
		if(mark != value) {
			mark = value;

			if(link != null) {
				link.mark(value);
			} else {
				for (TermNode n : subterms()) {
					n.term().mark(value);
				}
			}
		}
	}

	/**
	 * Checks for cycles via the link-fields. These cycles could lead to non-termination
	 * in some procedures like insertInto and they might occur when trying to unify two terms
	 *
	public boolean detectCycle() {
		boolean ret = detectCycleAux();
		mark(0); // unmark all subterms.
		return ret;
	}

	/**
	 * auxiliary function. recursively traverses all marked terms, marks active nodes with -1
	 * and thus detect cycles. Once a node has been invesitated its mark is reset to 0.
	 * @return
     *
	boolean detectCycleAux() {
		// subterms detected get value 1
		if(mark == 1) {
			return false; // already done, no cycle found here.
		} else if(mark == -1) {
			return true;
		} else { // mark == 1
			mark = -1; // mark to detect a cycle

			if(link != null) {
				boolean ret = link.detectCycleAux();
				mark = 1;
				return ret;
			} else {
				for (TermNode n : subterms()) {
					if (n.term().detectCycleAux()) {
						return true;
					}
				}

				// no cycle found in subterm
				mark = 1; // mark as done.
				return false;
			}
		}
	}

	/**
	 * Returns true if this and that result in a clash, ie, this and that are ununifiable
	 * @param that
	 * @return
     *
	public abstract boolean clash(Term that);

	/**
	 * Resets the link-field in this, in the linked terms and in all subterms
	 * recursively.
	 *
	public void unlink() {
		if(this.link != null) {
			this.link.unlink();
			this.link = null;

			for(TermNode n : subterms()) {
				n.term().unlink();
			}
		}
	}
	*/

	private int mark = 0; // for some algorithms, eg match and unification use this field

	/**
	 * Sets 'link' in variables such that  this and that term. If they are ununifiable, false is returned, otherwise,
	 * the link-field is set in each term.
	 * @param that
	 * @return
	 */
	public boolean match(Term that) {
		mark = 1;

		if(this == that) return true;

		if(this.link != null) {
			return this.link == that;
		} else {
			if(auxMatch(that)) {
				if(closed()) this.link = that;
				return true;
			} else {
				return false;
			}
		}
	}

	protected abstract boolean auxMatch(Term that);

	/**
	 * Since not in all terms link is set, I need mark in match and unmatch.
	 */
	public void unmatch() {
		if(mark == 1) {
			mark = 0;
			link = null;

			for(Term subterm : subterms()) {
				subterm.unmatch();
			}
		}
	}

	/**
	 * Sets 'link' in variables such that  this and that term. If they are ununifiable, false is returned, otherwise,
	 * the link-field is set in each term.
	 * @param that
	 * @return
	 */
	public boolean unify(Term that) {
		mark = 1; // mark both terms to know which ones were used in unify (necessary for ununify)
		that.mark = 1;

		if(this == that) return true;
		else if(this.link != null) return this.link.unify(that);
		else if(that.link != null) return this.unify(that.link);
		else if(this instanceof Var || !(that instanceof Var)) {
			if(auxUnify(that)) {
				// link is set inside auxUnify
				if(closed()) this.link = that;
				return true;
			} else {
				return false;
			}
		} else /*if(that instanceof Var)*/ {
			// same as in Var
			that.link = this;
			return true;
		}
	}

	protected abstract boolean auxUnify(Term that);

	/**
	 * Since not in all terms link is set, I need mark in match and unmatch.
	 */
	public void ununify() {
		if(mark == 1) {
			mark = 0;

			if(link != null) {
				link = null;
			}

			link = null;

			for(Term subterm : subterms()) {
				subterm.unmatch();
			}
		}
	}




	/**
	 * unifies the arguments in both iterators in the given order.
	 * If they are ununifiable, ie this procedure returnes false, the link-field
	 * will be reset.
	 * @param sarg
	 * @param targ
     * @return
     *
	protected static boolean unifyArgs(Iterator<TermNode> sarg, Iterator<TermNode> targ) {
		if(sarg.hasNext() && targ.hasNext()) {
			Term s = sarg.next().term();
			Term t = targ.next().term();

			if(!s.unify(t)) {
				return false;
			} else {
				if(!unifyArgs(sarg, targ)) {
					// if the remaining arguments are not unifiable
					// we have to reset this one.
					s.unlink();
					t.unlink();

					return false;
				} else {
					return true;
				}
			}
		} else {
			// both should not have any next, otherwise check clash.
			// sarg is in particular not a Var in this case!
			if(sarg.hasNext() || targ.hasNext()) throw new AssertionError();
			return true;
		}
	}

	/**
	 * Unifies this and that term. If they are ununifiable, false is returned, otherwise,
	 * the link-field is set in each term. Does *not* perform an occur-check!
	 * @param that
	 * @return
     *
	public boolean unify(Term that) {
		// follow links
		Term s = this;
		Term t = that;

		while(s.link != null) s = s.link;
		while(t.link != null) t = t.link;

		if(s == t) return true;

		if(s instanceof Var) {
			s.link = t;
			return true;
		} else if(t instanceof Var) {
			t.link = s;
			return true;
		} else {
			// check for a clash
			if(s.clash(that)) {
				return false;
			}

			if(unifyArgs(s.subterms().iterator(), t.subterms().iterator())) {
				// set link.
				s.link = t;
				return true;
			} else {
				return false;
			}
		}
	}
	*/

	// --- some static methods ---

	// empty iterator:
	public static final Iterable<Term> EMPTY_SUBTERMS =
			() -> new Iterator<Term>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public Term next() {
					throw new IllegalArgumentException();
				}
			};


	/**
	 * check whether these two iterables are identical.
	 * This one is static so that it can be used
	 * by all kinds of iterables.
	 * @param i1
	 * @param i2
	 * @return
	 */
	public static boolean checkIdenticalSubterms(Iterable<Term> i1, Iterable<Term> i2) {
		Iterator<Term> j1 = i1.iterator();
		Iterator<Term> j2 = i2.iterator();

		while(j1.hasNext() && j2.hasNext()) {
			if(j1.next() != j2.next()) return false;
		}

		// both are empty?
		return j1.hasNext() == j2.hasNext();
	}

}
