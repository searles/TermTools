package at.searles.terms;

import java.util.*;
import java.util.function.Function;

public abstract class Term extends TermList.Node {

	/*
	 * A note on these functions
	 *
	 * link is used for substitutions.
  	 * This class uses absolute debruijn indices.
	 * level is the nestedness of lambdas. A level of 3 means that there are 3 nested lambdas below or at the current term.
	 * maxLambdaIndex is the actual maximum index of a lambda variable. By comparing it to level it can be determined if
	 * there are any unbound lambda vars below it.
	 *
	 * normalform is used in connection with link and indicates that there is no 'link' set below or at the current level.
	 * This allows for some efficiency improvements in connection with inserting terms into a termlist.
	 *
	 */

	public Term link = null; // used for substitutions
	public boolean normalform = false; // if true, then it is guaranteed that no subterm has 'link' set.

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
	 * For terms without links, checking whether a term has some bound variables that is not yet bound
	 * by a lambda can be done via a simple comparison of maxLambdaIndex and level (method closed()).
	 * If there are links, there may be clashes, therefore I use 'insertedClosed' instead.
	 * [the same lambda variable might have different insertions depending on other args]
	 */
	protected boolean insertedClosed = false;  // true if there are no unbound lambda variables.

	//protected int cycleMark = 0;

	/**
	 * Checks for a cycle involving link fields. If there is a cycle, true is returned.
	 * @return
     *
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
	}*/

	/**
	 * This method prepares the insertion. It sets the insertLevel and insertedClosed-flag.
	 * @param lvs lambda variables that are collected in args.
	 * @return
     */
	void initLevel(List<LambdaVar> lvs) throws CycleException {
		if(insertLevel == -2) { // insert level of -2 indicates that we are stuck in a loop
			throw new CycleException(this);
		}

		try {
			if (insertLevel == -1) {
				if(link != null) {
					// here, recursion is used to find loops.
					insertLevel = -2; // -2 means that we are currently working on this one.
					link.initLevel(lvs);

					this.insertedClosed = link.insertedClosed;
					this.insertLevel = link.insertLevel; // ok, no loop here.
				} else if(!normalform) {
					// no need to do sth with normalforms.
					// normalforms are assumed to not have any link field set.
					insertLevel = -2;
					auxInitLevel(lvs);
					assert insertLevel != -2;
				}
			}
		} catch (CycleException o) {
			// there was a cycle. clean up the mess and be done.
			this.uninsert();
			throw o.append(this);
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
		} else if(list != parent || !normalform) {
			return inserted = this.auxInsert(list);
		} else {
			// in this case, this == link and parent == list.
			return inserted = this;
		}
	}

	// The following insert-functions are used if link is not set. They work the same way though
	// and also use the same fields. Advantage is that if the parents are equal, they might not
	// be executed at all.

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

			if(link != null && link != this) {
				link.uninsert();
			} else {
				for (int i = 0; i < arity(); ++i) {
					arg(i).uninsert();
				}
			}
		}
	}

	/**
	 * Returns the number of arguments
	 * @return
     */
	public abstract int arity();

	/**
	 * Returns the ith argument. i must be smaller than arity.
	 * @param p
	 * @return
     */
	public abstract Term arg(int p);

	/**
	 * Replaces the pth argument of this term by t. t must be in the same termlist
	 * as this.
	 * @param p
	 * @param t
     * @return
     */
	public abstract Term replace(int p, Term t);

	@FunctionalInterface
	public interface FilterFn {
		public Boolean apply(Term t, Integer i);
	}

	@FunctionalInterface
	public interface Op<A> {
		public A apply(Term t);
	}

	@FunctionalInterface
	public interface PostOp<A> {
		/**
		 * @param t
		 * @param args results of the arguments. If an argument was filtered, then args[i] might (!) be null.
		 *             It if was not filtered, then it will contain a concrete value.
         * @return
         */
		public A apply(Term t, List<A> args);
	}

	/**
	 * Returns a term in which all lambdas are incremented by d.
	 * @param d
	 * @return
     */
	Term updateLambda(int d) {
		if(maxLambdaIndex >= this.level) {
			// in this case, there are such lambda variables.
			int lowerLevel = this.level;

			return innermostOnDag(
					(t, p) -> (t.arg(p).maxLambdaIndex >= lowerLevel), // is there a lambda term below that must be updated?
					(t, args) -> {
						// fill in blanks
						for(int i = 0; i < t.arity(); ++i) {
							if(args.get(i) == null) args.set(i, t.arg(i));
						}

						if(t instanceof LambdaVar) {
							// fixme instanceof is ugly.
							// update lambda variable
							LambdaVar lv = (LambdaVar) t;

							// Because of filter, this one is definitely to be updated
							assert lv.index >= Term.this.level;

							return LambdaVar.create(Term.this.parent, lv.index + d);
						} else {
							return t.copy(Term.this.parent, args);
						}
					});
		} else {
			// otherwise, we are good.
			return this;
		}
	}

	/**
	 * This one replaces all occurences of some %n by %m. This is needed in beta reductions because
	 * reducing \1.(\0.%0) %1 would otherwise return \0.%1.
	 * @param oldIndex
	 * @param newIndex
     * @return
     */
	public Term replaceLambda(int oldIndex, int newIndex) {
		// do not replace if lambda newIndex is already bound in subterm.
		if(maxLambdaIndex >= oldIndex && level <= newIndex) {
			// in this case, there are such lambda variables.
			return innermostOnDag(
					(t, p) -> (t.arg(p).maxLambdaIndex >= oldIndex && t.arg(p).level > newIndex), // is there a lambda term below that must be updated?
					(t, args) -> {
						// fill in blanks
						for(int i = 0; i < t.arity(); ++i) {
							if(args.get(i) == null) args.set(i, t.arg(i));
						}

						if(t instanceof LambdaVar) {
							// fixme instanceof is ugly.

							LambdaVar lv = (LambdaVar) t;
							if(lv.index == oldIndex) {
								return LambdaVar.create(Term.this.parent, newIndex);
							} else {
								return lv;
							}
						} else {
							return t.copy(Term.this.parent, args);
						}
					});
		} else {
			// otherwise, we are good.
			return this;
		}
	}



	/** Applies an operation to all subterms on this term based on the arguments.
	 * @param filter
	 * @param postOp
	 * @param <A>
     * @return
     */
	public <A> A innermostOnDag(FilterFn filter, PostOp<A> postOp) {
		TreeSet<Term> queue = new TreeSet<>(TermList.CMP);
		TreeSet<Term> queue2 = new TreeSet<>(TermList.CMP);

		queue.add(this);

		while(!queue.isEmpty()) {
			Term t = queue.pollLast(); // fetch and remove.

			// it is guaranteed that due to the ordering, every node will occur only once.

			// add to second queue
			queue2.add(t);

			for(int i = 0; i < t.arity(); ++i) {
				// add based on filter.
				if(filter.apply(t, i)) queue.add(t.arg(i));
			}
		}

		TreeMap<Term, A> cache = new TreeMap<>(TermList.CMP);

		A a = null; // to store the last a.

		ArrayList<A> args = new ArrayList<A>();

		while(!queue2.isEmpty()) {
			Term t = queue2.pollFirst(); // now in the reverse order

			args.clear();
			args.ensureCapacity(t.arity());

			for(int i = 0; i < t.arity(); ++i) {
				args.add(cache.get(t.arg(i)));
			}

			a = postOp.apply(t, args);
			cache.put(t, a);
		}

		// return the last mapped element which is root.
		return a;
	}

	/** Applies an operation to all subterms on this term based on the arguments.
	 * This one uses recursion.
	 * @param filter
	 * @param postOp
	 * @param <A>
	 * @return
	 */
	public <A> A innermostOnTree(FilterFn filter, PostOp<A> postOp) {
		Stack<Term> stack = new Stack<>();
		Stack<ArrayList<A>> argStack = new Stack<>();

		Term t = this;
		ArrayList<A> args = new ArrayList<>(t.arity());
		int p = 0; // always same as args.size()

		// here we start.
		while(true) {
			while(p < t.arity()) {
				if(filter.apply(t, p)) {
					// recursive call
					stack.push(t);
					argStack.push(args);

					// reinit local variables
					t = t.arg(p);
					args = new ArrayList<>(t.arity());
					p = 0;
				} else {
					args.add(null);
					p++;
				}
			}

			A a = postOp.apply(t, args);

			if(stack.isEmpty()) {
				// we are done.
				return a;
			}

			args = argStack.pop();
			args.add(a);

			t  = stack.pop();
			p = args.size();
		}
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

			for(int i = 0; i < arity(); ++i) {
				arg(i).unmatch();
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

			for(int i = 0; i < arity(); ++i) {
				arg(i).unmatch();
			}
		}
	}

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


	/**
	 * If link is set, then go to last linked term and check whether it is a normalform.
	 * If it isn't one then the normalizing-procedure is stuck in a loop.
	 * If link is not set, then resolve it.
	 * @return
     */
	Term resolveLinks() {
		// if link in u is already set, it must have already been rewritten.
		if(this.link != null) {
			// go to last link
			while(this.link.link != null && this.link != this.link.link) this.link = this.link.link;

			// if it isn't a normalform, then sry, a loop.
			if(!this.link.normalform) throw new CycleException(this.link);

			return this.link;
		} else {
			return this;
		}
	}

	/**
	 * Normalizes a term t using the term function fn. The reducts are stored inside the same termlist as t and the
	 * link-fields are set to reducts. link does not represent the 1-step reduction.
	 *
	 * If a loop occurs, an exception is thrown (fn's may
	 * use different means to deal with such to recover from such mistakes). Terms that were not rewritten
	 * (either because they are in a subterm such that it was not necessary or because they ended in a loop)
	 * point to null.
	 * @param fn
	 * @return
	 */
	public Term normalize(TermFn fn) {
		// if link in u is already set, it must have already been rewritten.
		Term t = resolveLinks();

		// fixme I should mark a term before I rewrite it...

		// rewrite it at root as long as possible.
		while (!t.normalform) {
			Term u = null;

			boolean subtermRewritten = false;

			for (int i = 0; i < t.arity(); ++i) {
				t.arg(i).normalize(fn);
				if(t.arg(i).link != null) subtermRewritten = true;
			}

			if (subtermRewritten) {
				// some subterm was rewritten/normalized.
				u = t.parent.insert(t);
			} else {
				// force cycle
				try {
					t.link = t;
					u = fn.apply(t, t.parent);
				} finally {
					t.link = null;
				}

				if(u == null) {
					t.normalform = true;
					return t;
				}
			}
			// normalize was not called yet on this term.

			// u contains a reduct of t.

			// what if it is a self-loop?
			if(t == u) throw new CycleException(t);

			// if link in u is already set, it must have already been rewritten.
			u = u.resolveLinks();

			// connect it now that we are sure that no loops would be introduced.
			t = t.link = u;

			// if u (now t) is already a normalform, the loop will terminate.
		}

		return t;
	}


}
