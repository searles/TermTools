package at.searles.terms;

import java.util.*;

public abstract class Term extends TermList.Node {

	public Term link = null; // used for substitutions
	public boolean normalform = false;

	public final int level; // lambda level
	public final int maxLambdaIndex; // max lambda index in a subterm, -1 if no lambda variable below.

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
	private Term inserted = null; // used by insertInto.

	/**
	 * The lambda level when the term is inserted
	 */
	protected int insertLevel = -1; // lambda level of the inserted term.

	/**
	 * This one is set in initLevel if in some subterm link is set. Allows a speedup when inserting into the same
	 * termlist
	 */
	protected boolean linkSet = false;

	/**
	 * For terms without links, checking whether a term has some bound variables that is not yet bound
	 * by a lambda can be done via a simple comparison of maxLambdaIndex and level (method closed()).
	 * If there are links, there may be clashes, therefore I use 'insertedClosed' instead.
	 * [the same lambda variable might have different insertions depending on other args]
	 */
	protected boolean insertedClosed = false;  // true if there are no unbound lambda variables.

	protected int cycleMark = 0;

	/**
	 * Checks for a cycle involving link fields. If there is a cycle, true is returned.
	 * @return
     */
	public boolean containsCycle() {
		boolean ret = auxContainsCycle();
		unmarkCycles();
		return ret;
	}

	protected boolean auxContainsCycle() {
		if(cycleMark == 1) {
			return false;
		} else if(cycleMark == 2) {
			cycleMark = 0;
			return true;
		} else {
			cycleMark = 2;

			if(link != null) {
				if(link.auxContainsCycle()) {
					return true;
				} else {
					cycleMark = 1;
					return false;
				}
			} else {
				int i = 0;
				for(Term arg : args()) {
					i++;
					if(arg.containsCycle()) {
						return true;
					}
				}
				cycleMark = 1;
				return false;
			}
		}
	}

	protected void unmarkCycles() {
		if(cycleMark != 0) {
			cycleMark = 0;
			if(link != null) {
				link.unmarkCycles();
			} else {
				for(Term arg : args()) arg.unmarkCycles();
			}
		}
	}

	/**
	 * This method prepares the insertion. It sets the insertLevel and insertedClosed-flag.
	 * @param lvs lambda variables that are collected in args.
	 * @return
     */
	void initLevel(List<LambdaVar> lvs) throws CycleException {
		if(cycleMark == 1) {
			throw new CycleException(this);
		}

		try {
			if (insertLevel == -1) {
				if (link != null) {
					// here, recursion is used to find loops.
					cycleMark = 1;
					link.initLevel(lvs);
					cycleMark = 0;

					this.insertedClosed = link.insertedClosed;
					this.insertLevel = link.insertLevel;
					this.linkSet = true;
				} else {
					// fixme: no need to do sth with normalforms if parent is identical
					cycleMark = 1;
					auxInitLevel(lvs);
					cycleMark = 0;
				}

				// link == this marks a normalform
			}
		} catch (CycleException o) {
			o.append(this);
			this.uninsert();
			throw o;
		}
	}

	public abstract void auxInitLevel(List<LambdaVar> lvs);

	protected abstract Term auxInsert(TermList list);

	/**
	 * Inserts the termqueue in 'this' into s. The is-field in 'this' is used to store already inserted nodes.
	 * It follows links in terms.
	 * @param list the list in which this is inserted. null causes unexpected behaviour.
	 * @return the inserted term.
	 */
	public Term insertInto(TermList list) {
		// check whether inserted should be set.
		// Here, it is checked whether all elements in lvStack (ie all bound variables)
		// are set to identity.
		// This could be optimized by limiting this to all bound variables that are actually used in a subterm,
		// but such an optimization might be memory consuming.

		if(insertedClosed && inserted != null) {
			return inserted;
		} else if(link != null) {
			// link is this can be used for normalizations
			// resolve link.
			return inserted = link.insertInto(list);
		} else if(linkSet || list != parent) {
			return inserted = this.auxInsert(list);
		} else {
			// in this case, this == link and parent == list.
			return inserted = this;
		}
	}

	/**
	 * insertInto sets the inserted-field to equivalent terms in another termlist
	 * In order to unset the inserted-field, the tree-term-structure is traversed
	 * in this recursive function and all insert-fields are cleared (set to null), insertLevel is reset and so is "insertedClosed".
	 */
	void uninsert() {
		if(insertLevel != -1) {
			inserted = null;
			insertLevel = -1;
			insertedClosed = false;
			linkSet = false;
			cycleMark = 0;

			if(link != null && link != this) {
				link.uninsert();
			} else {
				for (Term subterm : args()) {
					subterm.uninsert();
				}
			}
		}
	}

	// next shallow insert. Faster alternative
	/*Term shallowInsertInto(TermList target, Map<String, String> renaming) {
		if(inserted == null) {
			inserted = auxShallowInsert(target, renaming);
			if(this.link != null) inserted.link = link;
		}

		return inserted;
	}

	abstract Term auxShallowInsert(TermList target, Map<String, String> renaming);
	*/


	/*void unshallowInsert() {
		if(inserted != null) {
			inserted = null;
			for(Term arg : args()) {
				arg.unshallowInsert();
			}
		}
	}*/

	public static abstract class TermIterator implements Iterator<Term> {
		/**
		 * Replaces the last returned term. The termlist that is used is 'parent'.
		 * @param u Term to be inserted.
         * @return
         */
		public abstract Term replace(Term u);
	}

	/**
	 * Returns an iterable of all direct subterms
	 * @return
	 */
	public Iterable<Term> args() {
		return () -> argsIterator();
	}

	/**
	 * Returns
	 * @return
     */
	public abstract TermIterator argsIterator();

	public Iterable<Term> dag() {
		return () -> dagIterator();
	}

	/**
	 * Returns an iterable of all subterms. Each subterm is returned only once.
	 * The iterator is not a TreeIterator because it does not support replacing terms.
	 * The order in which subterms are returned is not fixed.
	 */
	public Iterator<Term> dagIterator() {
		// I use a tree set to add args
		final TreeSet<Term> queue = new TreeSet<>(TermList.CMP);
		queue.add(this);

		return new Iterator<Term>() {
			@Override
			public boolean hasNext() {
				return !queue.isEmpty();
			}

			@Override
			public Term next() {
				Term t = queue.pollLast();

				for (Term u : t.args()) {
					queue.add(u);
				}

				return t;
			}
		};
	}

	public Iterable<Term> tree() {
		return () -> treeIterator();
	}

	/**
	 * Returns an iterable of all subterms.
	 */
	public TermIterator treeIterator() {
		return new TermIterator() {

			boolean rootReturned = false;
			TermIterator subterms = argsIterator();
			TermIterator subtermIterator = null;

			@Override
			public boolean hasNext() {
				// this one is generic for all possible strategies.
				return !rootReturned || subterms.hasNext() || (subtermIterator != null && subtermIterator.hasNext());
			}

			@Override
			public Term next() {
				if(!rootReturned) {
					rootReturned = true;
					return Term.this;
				} else {
					if(subtermIterator == null || !subtermIterator.hasNext()) {
						subtermIterator = subterms.next().treeIterator();
					}

					return subtermIterator.next();
				}
			}

			@Override
			public Term replace(Term u) {
				if(rootReturned && subtermIterator == null) {
					// we just returned the root but not a subterm yet.
					return u;
				} else {
					return subterms.replace(subtermIterator.replace(u));
				}
			}
		};

	}

	/**
	 * Checks whether this is equal to t where for the args
	 * identity is used. This method is tailored to properly work with the
	 * insert algorithm to search for terms that were created with the copy method.
	 * @param t
	 * @return
     */
	protected abstract boolean eq(Term t);


	private int mark = 0; // for some algorithms, eg match and unification and cycleMark use this field

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

			for(Term subterm : args()) {
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

			for(Term subterm : args()) {
				subterm.unmatch();
			}
		}
	}


	// --- some static methods ---

	// empty iterator:
	public static final TermIterator EMPTY_SUBTERMS =
			new TermIterator() {
				@Override
				public Term replace(Term u) {
					throw new IllegalArgumentException();
				}

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
	public static boolean checkIdenticalSubterms(Term[] i1, Term[] i2) {
		if(i1.length == i2.length) {
			for(int i = 0; i < i1.length; ++i) {
				if(i1[i] != i2[i]) return false;
			}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a copy of this term and inserts it into the termlist (or finds an equivalent term). The arguments in
	 * the argument must be inside list. The resulting term will be also put into list.
	 * @param args
	 * @return
     */
	public abstract Term copy(TermList list, List<Term> args);

}
