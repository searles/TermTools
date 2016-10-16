package at.searles.terms;

import java.util.*;

public abstract class Term extends TermList.Node {

	/*
	 * A note on these functions
	 *
	 * link is used for substitutions.

	 * normalform is used in connection with link and indicates that there is no 'link' set below or at the current level.
	 * This allows for some efficiency improvements in connection with inserting terms into a termlist.
	 *
  	 * This class uses relative debruijn indices.
  	 *
	 */

	/**
	 * A replacement for this term. This one can be used for fast substitutions.
	 */
	public Term link = null; // used for substitutions

	/**
	 * If this flag is set, then it is guaranteed that no subterm has "link" set.
	 */
	public boolean normalform = false; // if true, then it is guaranteed that no subterm has 'link' set.

	@Override
	public String toString() {
		// Just a forward (so that various annotations can be added during testing...)
		return str(new LinkedList<>());
	}

	/**
	 * Simple toString() without consideration of link.
	 * @param lambdaVars
	 * @return
     */
	protected abstract String str(LinkedList<String> lambdaVars);

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

	// The following fields are used for the insert-algorithm
	/**
	 * Inserts the term in 'this' into the termlist target. It resolves
	 * link fields while doing this.
	 * @param target the list in which this is inserted. null causes unexpected behaviour.
	 * @return the inserted term.
	 */
	public Term insertInto(TermList target) {
		// FIXME could use innermostOnDAG with a filter based on whether link is set. If it is, then
		// FIXME do a recursive call.
		if(link != null) {
			// link is this can be used for normalizations
			// resolve link.
			return link.insertInto(target);
		} else {
			ArrayList<Term> args = null;
			if(arity() > 0) {
				// if there are arguments, they must be inserted first.
				args = new ArrayList<>(arity());

				for(int i = 0; i < arity(); ++i) {
					args.add(arg(i).insertInto(target));
				}
			}

			Term ret = copy(target, args);

			return ret;
		}
	}



	@FunctionalInterface
	public interface FilterFn {
		public boolean apply(Term t, int i);
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

		int maxArity = 0;

		while(!queue.isEmpty()) {
			Term t = queue.pollLast(); // fetch and remove.

			if(t.arity() > maxArity) maxArity = t.arity();

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

		// create only one array list and reuse it.
		ArrayList<A> args = new ArrayList<>(maxArity);

		while(!queue2.isEmpty()) {
			Term t = queue2.pollFirst(); // now in the reverse order

			args.clear();

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
				/*if(closed()) */this.link = that;
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
				/*if(closed()) */this.link = that;
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

	protected Term shift(int shift, int cutoff) {
		return innermostOnDag((v, p) -> !(v instanceof Lambda),
				(v, args) -> {
					if(v instanceof Lambda) {
						Lambda lam = (Lambda) v;

						// these ones don't have arguments.
						return Lambda.create(parent, lam.t.shift(shift, cutoff + 1));
					} else if(v instanceof LambdaVar) {
						LambdaVar lv = (LambdaVar) v;
						if(lv.index >= cutoff) {
							return LambdaVar.create(parent, lv.index + shift, parent);
						}
					}

					// fill in blanks in args
					for(int i = 0; i < v.arity(); ++i) {
						if(args.get(i) == null) {
							args.set(i, v.arg(i));
						}
					}

					return v.copy(parent, args);
				});
	}

	/**
	 * Method for substituting lambda variables. Used mainly for beta reductions
	 * @param u Term to be inserted for lambda variable
	 * @return
	 */
	protected Term substitute(Term u, int index) {
		// fixme put into Term
		return innermostOnDag((t, p) -> !(t instanceof Lambda),
				(t, args) -> {
					if(t instanceof Lambda) {
						Lambda lam = (Lambda) t;
						Term tSubst = lam.t.substitute(u.shift(1, 0), index + 1);
						return Lambda.create(parent, tSubst);
					} else if(t instanceof LambdaVar) {
						if(((LambdaVar) t).index == index) {
							return u;
						}
					}

					// in all other cases

					// fill in blanks in args
					for(int i = 0; i < t.arity(); ++i) {
						if(args.get(i) == null) {
							args.set(i, t.arg(i));
						}
					}

					return t.copy(parent, args);
				});
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
}
