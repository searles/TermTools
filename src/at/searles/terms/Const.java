package at.searles.terms;

import java.util.List;

public class Const<T> extends Term {

	public static <T> TermList.Node create(TermList list, T value) {
		return list.assertInList(new Const(value), null);
	}

	final T value;

	public Const(T value) {
		super(0, -1);
		this.value = value;
	}

	/**
	 * Special method for const. Returns the wrapped value
	 * @return
     */
	public T value() {
		return value;
	}

	@Override
	public Iterable<Term> subterms() {
		return Term.EMPTY_SUBTERMS;
	}

	@Override
	public boolean eq(Term t) {
		return t instanceof Const && value.equals(((Const) t).value);
	}

	@Override
	protected boolean auxMatch(Term that) {
		return (that instanceof Const) && value.equals(((Const) that).value);
	}

	@Override
	protected boolean auxUnify(Term that) {
		// same as match...
		return auxMatch(that);
	}

	/*@Override
	public Term copy(List<Term> subterms) {
		return this;
	}*/

	protected String str() {
		return value.toString();
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
		insertLevel = 0;
		insertedClosed = true;
	}

	@Override
	TermList.Node auxInsert(TermList list) {
		return list.assertInList(this, null);
	}
}
