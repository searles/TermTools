package at.searles.terms;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class App extends Term {

	/**
	 * Constructs and inserts nodes into list. l and r must be in list! This one
	 * does not call insert, hence no insert-fields are set. It is intended to build
	 * a term without the overhead of calling insert multiple times (eg useful
	 * for parsers).
	 * @param list
	 * @param l
	 * @param r
     * @return
     */
	public static Term create(TermList list, Term l, Term r) {
		// l and r must be in list!
		return list.findOrAppend(new App(l, r),
				l.index > r.index ? l : r);
	}

	Term l;
	Term r;

	App(Term l, Term r) {
		super(Math.max(l.level, r.level), Math.max(l.maxLambdaIndex, r.maxLambdaIndex));
		this.l = l;
		this.r = r;
	}

	@Override
	public TermIterator argsIterator() {
		return new TermIterator() {
			int i = -1;

			@Override
			public boolean hasNext() {
				return i < 1;
			}

			@Override
			public Term next() {
				i++;
				return i == 0 ? l : r;
			}

			@Override
			public Term replace(Term u) {
				if(u.parent != parent) throw new IllegalArgumentException();

				return App.create(parent, i == 0 ? u : l, i == 1 ? u : r);
			}
		};
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		l.initLevel(lvs);
		r.initLevel(lvs);

		insertLevel = Math.max(l.insertLevel, r.insertLevel);
		insertedClosed = l.insertedClosed && r.insertedClosed;
	}

	@Override
	protected Term auxInsert(TermList list) {
		Term ln = l.insertInto(list);
		Term rn = r.insertInto(list);

		return App.create(list, ln, rn);
	}

	/*@Override
	Term auxShallowInsert(TermList target, Map<String, String> renaming) {
		return create(target, l.shallowInsertInto(target, renaming), r.shallowInsertInto(target, renaming));
	}*/

	@Override
	public boolean eq(Term t) {
		return t instanceof App && l == ((App) t).l && r == ((App) t).r;
	}

	@Override
	protected boolean auxMatch(Term that) {
		if(that instanceof App) {
			App a = (App) that;
			if(l.match(a.l)) {
				if(r.match(a.r)) {
					return true;
				} else {
					l.unmatch();
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	protected boolean auxUnify(Term that) {
		if(that instanceof App) {
			App a = (App) that;
			if(l.unify(a.l)) {
				if(r.unify(a.r)) {
					return true;
				} else {
					l.ununify();
					a.l.ununify();
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		return App.create(list, args.get(0), args.get(1));
	}

	protected String str() {
		String sl = l.toString();
		String sr = r.toString();

		if(l instanceof Lambda) {
			sl = "(" + sl + ")";
		}

		if(r instanceof App) {
			sr = "(" + r + ")";
		}
		return sl + " " + sr;
	}
}
