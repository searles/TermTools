package at.searles.terms;

import java.util.Iterator;
import java.util.List;

public class Var extends Term {

	public static TermList.Node create(TermList list, String id) {
		return list.assertInList(new Var(id), null);
	}

	String id;

	public Var(String id) {
		super(0, -1);
		this.id = id;
	}

	@Override
	public Iterable<Term> subterms() {
		return () -> new Iterator<Term>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Term next() {
				return null;
			}
		};
	}

	@Override
	public boolean eq(Term t) {
		// ignore subterms because there are none.
		return t instanceof Var && id.equals(((Var) t).id);
	}

	@Override
	protected boolean auxMatch(Term that) {
		// non-linearity is checked via link which is implemented in match-method in Term.
		return true;
	}

	@Override
	protected boolean auxUnify(Term that) {
		return true;
	}

	/*@Override
	public Term copy(List<Term> subterms) {
		return this;
	}*/

	protected String str() {
		return id;
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
		insertLevel = 0;
		insertedClosed = true;
	}


	@Override
	TermList.Node auxInsert(TermList list) {
		// fixme maybe speed up?
		return list.assertInList(this, null);
	}
}
