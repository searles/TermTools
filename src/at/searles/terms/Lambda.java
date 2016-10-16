package at.searles.terms;

import java.util.*;

public class Lambda extends Term {

	public static Term create(TermList list, Term t) {
		// if scope is different from list, then the term must be updated.

		return list.findOrAppend(new Lambda(t), t);
	}

	final Term t;

	public Lambda(Term t) {
		this.t = t;
	}

	public Term beta(Term u) {
		// \x.t u -> ([u^1/0]t)^-1
		// see eg http://ttic.uchicago.edu/~pl/classes/CMSC336-Winter08/lectures/lec5.pdf
		Term uShift = u.shift(1, 0);
		Term tSubst = t.substitute(uShift, 0);
		Term ret = tSubst.shift(-1, 0);

		return ret;
	}

	@Override
	public int arity() {
		return 1;
	}

	@Override
	public Term arg(int p) {
		return this.t;
	}

	@Override
	public Term replace(int p, Term t) {
		return create(this.parent, t);
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
		return false;
	}


	@Override
	protected boolean auxUnify(Term that) {
		// this only works if terms are in some normalform (like beta-eta NF).
		return false;
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		Term copy_t = args.get(0);

		// fixme depends on scope!

		return Lambda.create(list, copy_t);
	}

	protected String str(LinkedList<String> vars) {
		char ch = (char) ('a' + vars.size());

		String v = Character.toString(ch);

		/*if(vars.size() >= 26) {
			v += vars.size() / 26;
		}*/

		vars.addFirst(v);
		String ret = "\\" + v + "." + t.str(vars);
		vars.removeFirst();
		return ret;
	}

}
