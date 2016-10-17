package at.searles.terms;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class LambdaVar extends Term {

	public static LambdaVar create(TermList list, int index, TermList scope) {
		return (LambdaVar) list.findOrAppend(new LambdaVar(index, scope), null);
	}

	final TermList scope;
	final int index;
	Stack<Integer> insertIndices = new Stack<Integer>();

	public LambdaVar(int index, TermList scope) {
		this.index = index;
		this.scope = scope;
	}

	@Override
	public boolean eq(Term t) {
		// NEW: Identity of parent is part of it.
		return t instanceof LambdaVar && this.index == ((LambdaVar) t).index && this.scope == ((LambdaVar) t).scope;
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
		return list == parent ? this : LambdaVar.create(list, index, scope);
	}

	/*@Override
	public Term copy(List<Term> args) {
		return this;
	}*/

	protected String str(LinkedList<String> vars) {
		if(scope != parent) {
			return "%" + index + ":" + scope.id;
		} else if(0 <= index && index < vars.size()) {
			return vars.get(index);
		} else {
			return "%" + index;
		}
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

	@Override
	protected Term copyInserted(TermList target) {
		return copy(target, null);
	}

	/*@Override
	protected Term auxShallowInsert(TermList list, Map<String, String> renaming) {
		// avoid creating too many objects.
		return LambdaVar.create(list, index);
	}*/
}
