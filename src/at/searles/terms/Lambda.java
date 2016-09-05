package at.searles.terms;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Lambda extends Term {

	public static TermList.Node create(TermList list, TermList.Node t) {
		TermList.Node v = LambdaVar.create(list, t.value.level); // it is a new node. No link is set.

		return list.assertInList(new Lambda(t.value, (LambdaVar) v.value),
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
	public Iterable<Term> subterms() {
		return () -> new Iterator<Term>() {
			int hasNext = -1;

			@Override
			public boolean hasNext() {
				return hasNext < 1;
			}

			@Override
			public Term next() {
				hasNext++;
				return hasNext == 0 ? lv : t;
			}
		};
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
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
	TermList.Node auxInsert(TermList list) {
		lv.insertIndices.push(insertLevel - 1);
		TermList.Node ret = Lambda.create(list, t.insertInto(list));
		lv.insertIndices.pop();

		return ret;
	}

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

	protected String str() {
		// debruijn index is by 1 too high (which is better for some algorithms!)
		return "\\" + (level - 1) + "." + t;
	}
}
