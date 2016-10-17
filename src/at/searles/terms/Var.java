package at.searles.terms;

import java.util.LinkedList;
import java.util.List;

public class Var extends Term {

	public static Term create(TermList list, String id) {
		return list.findOrAppend(new Var(id), null);
	}

	String id;

	public Var(String id) {
		this.id = id;
	}

	@Override
	public boolean eq(Term t) {
		// ignore args because there are none.
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

	@Override
	public Term copy(TermList list, List<Term> args) {
		return list == parent ? this : Var.create(list, id);
	}

	/*@Override
	public Term copy(List<Term> args) {
		return this;
	}*/

	protected String str(LinkedList<String> strings) {
		return id;
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
		String newId;
		if(renaming != null) {
			newId = renaming.get(id);
			if(newId == null) newId = id;
		} else {
			newId = id;
		}

		return create(list, newId);
	}*/
}
