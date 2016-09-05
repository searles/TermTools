package at.searles.terms;

import java.util.Iterator;
import java.util.List;

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
	public static TermList.Node create(TermList list, TermList.Node l, TermList.Node r) {
		// l and r must be in list!
		return list.assertInList(new App(l.value, r.value),
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
	public Iterable<Term> subterms() {
		return () -> new Iterator<Term>() {
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
			};
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
		l.initLevel(lvs);
		r.initLevel(lvs);

		insertLevel = Math.max(l.insertLevel, r.insertLevel);
		insertedClosed = l.insertedClosed && r.insertedClosed;
	}

	@Override
	TermList.Node auxInsert(TermList list) {
		TermList.Node ln = l.insertInto(list);
		TermList.Node rn = r.insertInto(list);

		return App.create(list, ln, rn);
	}

	@Override
	public boolean eq(Term t) {
		return t instanceof App && Term.checkIdenticalSubterms(this.subterms(), t.subterms());
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
