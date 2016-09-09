package at.searles.terms;

import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LambdaVar extends Term {

	public static Term create(TermList list, int index) {
		return list.findOrAppend(new LambdaVar(index), null);
	}

	final int index;
	Stack<Integer> insertIndices = new Stack<Integer>();

	public LambdaVar(int index) {
		super(0, index);
		this.index = index;
	}

	@Override
	public TermIterator argsIterator() {
		return Term.EMPTY_SUBTERMS;
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

	@Override
	protected boolean auxUnify(Term that) {
		// link in this must be set and point to that. But this is already checked before in match-method.
		return false;
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		return LambdaVar.create(list, index);
	}

	/*@Override
	public Term copy(List<Term> args) {
		return this;
	}*/

	protected String str() {
		return "%" + index;
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		lvs.add(this);
		insertLevel = 0;
		insertedClosed = false;
		linkSet = false;
	}

	@Override
	protected Term auxInsert(TermList list) {
		// avoid creating too many objects.
		int newIndex = insertIndices.isEmpty() ? index : insertIndices.peek();
		return list.findOrAppend(newIndex == index ? this : new LambdaVar(newIndex), null);
	}

	/*@Override
	protected Term auxShallowInsert(TermList list, Map<String, String> renaming) {
		// avoid creating too many objects.
		return LambdaVar.create(list, index);
	}*/
}
