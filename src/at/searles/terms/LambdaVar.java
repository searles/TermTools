package at.searles.terms;

import java.util.List;
import java.util.Stack;

public class LambdaVar extends Term {

	public static TermList.Node create(TermList list, int index) {
		return list.assertInList(new LambdaVar(index), null);
	}

	final int index;
	Stack<Integer> insertIndices = new Stack<Integer>();

	public LambdaVar(int index) {
		super(0, index);
		this.index = index;
	}

	@Override
	public Iterable<Term> subterms() {
		return EMPTY_SUBTERMS;
	}

	@Override
	public boolean eq(Term t) {
		return t instanceof LambdaVar && this.index == ((LambdaVar) t).index;
	}

	@Override
	protected boolean auxMatch(Term that) {
		// link in this must be set and point to that. But this is already checked before in match-method.
		return false;
	}

	/*@Override
	public Term copy(List<Term> subterms) {
		return this;
	}*/

	protected String str() {
		return "%" + index;
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
		lvs.add(this);
		insertLevel = 0;
		insertedClosed = false;
	}

	@Override
	TermList.Node auxInsert(TermList list) {
		// avoid creating too many objects.
		int newIndex = insertIndices.isEmpty() ? index : insertIndices.peek();
		return list.assertInList(newIndex == index ? this : new LambdaVar(newIndex), null);
	}
}
