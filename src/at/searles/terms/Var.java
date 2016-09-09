package at.searles.terms;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Var extends Term {

	public static Term create(TermList list, String id) {
		return list.findOrAppend(new Var(id), null);
	}

	String id;

	public Var(String id) {
		super(0, -1);
		this.id = id;
	}

	@Override
	public TermIterator argsIterator() {
		return Term.EMPTY_SUBTERMS;
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
		return Var.create(list, id);
	}

	/*@Override
	public Term copy(List<Term> args) {
		return this;
	}*/

	protected String str() {
		return id;
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		insertLevel = 0;
		insertedClosed = true;
		linkSet = false;
	}


	@Override
	protected Term auxInsert(TermList list) {
		return create(list, id);
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
