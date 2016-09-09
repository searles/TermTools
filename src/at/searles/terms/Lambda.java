package at.searles.terms;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Lambda extends Term {

	public static Term create(TermList list, Term t) {
		Term v = LambdaVar.create(list, t.level); // it is a new node. No link is set.

		return list.findOrAppend(new Lambda(t, (LambdaVar) v),
				t.index > v.index ? t : v);
	}

	final Term t;
	final LambdaVar lv;

	public Lambda(Term t, LambdaVar lv) {
		super(t.level + 1, t.maxLambdaIndex);
		this.t = t;
		assert lv.index == level - 1;
		this.lv = lv;
	}

	@Override
	public TermIterator argsIterator() {
		return new TermIterator() {
			boolean hasNext = true;

			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public Term next() {
				hasNext = false;
				return t;
			}

			@Override
			public Term replace(Term u) {
				if(u.parent != parent) throw new IllegalArgumentException();
				return create(parent, u);
			}
		};
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		t.initLevel(lvs);

		insertLevel = t.insertLevel + 1;

		// remove the variable from lvs
		ListIterator<LambdaVar> i = lvs.listIterator();
		while(i.hasNext()) {
			if(i.next() == lv) {
				i.remove();
				break;
			}
		}

		insertedClosed = lvs.isEmpty();
	}

	@Override
	protected Term auxInsert(TermList list) {
		lv.insertIndices.push(insertLevel - 1);
		Term ret = Lambda.create(list, t.insertInto(list));
		lv.insertIndices.pop();

		return ret;
	}

	/*@Override
	protected Term auxShallowInsert(TermList list, Map<String, String> renaming) {
		return Lambda.create(list, t.shallowInsertInto(list, renaming));
	}*/

	@Override
	public boolean eq(Term t) {
		return t instanceof Lambda && this.t == ((Lambda) t).t;
	}

	@Override
	protected boolean auxMatch(Term that) {
		// this only works if terms are in some normalform (like beta-eta NF).
		if(that instanceof Lambda) {
			Lambda l = (Lambda) that;

			lv.link = l.lv; // careful, lambda variables may have different indices.

			boolean ret = t.match(l.t);

			// unset link that was set before
			lv.link = null;

			return ret;
		}

		return false;
	}


	@Override
	protected boolean auxUnify(Term that) {
		// this only works if terms are in some normalform (like beta-eta NF).
		if(that instanceof Lambda) {
			Lambda l = (Lambda) that;

			lv.link = l.lv; // careful, lambda variables may have different indices.

			boolean ret = t.unify(l.t);

			// unset link that was set before
			lv.link = null;

			return ret;
		}

		return false;
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		return Lambda.create(list, args.get(0));
	}

	protected String str() {
		// debruijn index is by 1 too high (which is better for some algorithms!)
		return "\\" + (level - 1) + "." + t;
	}
}
