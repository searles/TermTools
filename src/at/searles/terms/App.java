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
		// fixme move this one to termlist
		if(l.parent != list || r.parent != list) throw new IllegalArgumentException();
		// l and r must be in list!
		return list.findOrAppend(new App(l, r),
				l.index > r.index ? l : r);
	}

	private final Term l;
	private final Term r;

	public App(Term l, Term r) {
		super(Math.max(l.level, r.level), Math.max(l.maxLambdaIndex, r.maxLambdaIndex));

		if(l.parent != r.parent) throw new IllegalArgumentException();

		this.l = l;
		this.r = r;
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

	@Override
	public int arity() {
		return 2;
	}

	@Override
	public Term arg(int p) {
		return p == 0 ? l : r;
	}

	@Override
	public Term replace(int p, Term t) {
		// FIXME this one does not work if there is a lambda term parallel to it!
		return create(parent, p == 0 ? t : l, p == 1 ? t : r);
	}

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
		// I might have to update lambda indices.
		Term copy_l = args.get(0);
		Term copy_r = args.get(1);

		int copy_level = Math.max(copy_l.level, copy_r.level);

		/*if(copy_level != level) {
			// yup, I have to update the lambda indices in variables.
			copy_l = copy_l.updateLambda(copy_level - copy_l.level);
			copy_r = copy_r.updateLambda(copy_level - copy_r.level);
		}*/

		return App.create(list, copy_l, copy_r);
	}

	protected String str() {
		String sl = l.toString();
		String sr = r.toString();

		if(l instanceof Lambda) {
			sl = "(" + sl + ")";
		}

		if(r instanceof App || r instanceof Lambda) {
			sr = "(" + r + ")";
		}
		return sl + " " + sr;
	}
}
