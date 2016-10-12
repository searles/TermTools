package at.searles.terms;

import java.util.List;
import java.util.Map;

public class Const<T> extends Term {

	public static <T> Term create(TermList list, T value) {
		return list.findOrAppend(new Const<>(value), null);
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

	@Override
	public Term copy(TermList list, List<Term> args) {
		return Const.create(list, this.value);
	}

	/*@Override
	public Term copy(List<Term> args) {
		return this;
	}*/

	protected String str() {
		return value.toString();
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		insertLevel = 0;
		insertedClosed = true;
	}

	@Override
	protected Term auxInsert(TermList list) {
		return list.findOrAppend(new Const<>(value), null);
	}

	@Override
	public int arity() {
		return 0;
	}

	@Override
	public Term arg(int p) {
		return null;
	}

	@Override
	public Term replace(int p, Term t) {
		return null;
	}

	/*@Override
	Term auxShallowInsert(TermList target, Map<String, String> renaming) {
		return target.findOrAppend(new Const<>(value), null);
	}*/
}
