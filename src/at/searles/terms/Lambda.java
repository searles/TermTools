package at.searles.terms;

import java.util.*;

public class Lambda extends Term {

	public static Term create(TermList list, Term t) {
		Term v = LambdaVar.create(list, t.level); // it is a new node. No link is set.

		return list.findOrAppend(new Lambda(t, (LambdaVar) v),
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

	/**
	 * Method for beta reduction
	 * @param u Term to be inserted for lambda variable
	 * @return
	 */
	public Term beta2(Term u) {
		// use innermost-dag.
		// fixme I could rather use a 'replaceSubterm'-method?

		// I need to replace the lambda variable with index "level - 1"

		return t.innermostOnDag(
				(t, i) -> t.arg(i).maxLambdaIndex >= level - 1, // no need to change this one.
				(t, args) -> {
					for(int i = 0; i < args.size(); ++i) {
						if(args.get(i) == null) args.set(i, t.arg(i));
						// fill in the blanks.
					}

					if(t == lv) {
						return u;
					} else {
						return t.copy(parent, args);
					}
				}
		);
	}

	/**
	 * Method for beta reduction
	 * @param u Term to be inserted for lambda variable
	 * @return
	 */
	public Term beta(Term u) {
		lv.link = u;

		Term ret = parent.insert(t);

		lv.link = null;
		return ret;

		/*
		// use innermost-dag.
		// fixme I could rather use a 'replaceSubterm'-method?

		// I need to replace the lambda variable with index "level - 1"

		return t.innermostOnDag(
				(t, i) -> t.arg(i).maxLambdaIndex >= level - 1, // no need to change this one.
				(t, args) -> {
					for(int i = 0; i < args.size(); ++i) {
						if(args.get(i) == null) args.set(i, t.arg(i));
						// fill in the blanks.
					}

					if(t == lv) {
						return u;
					} else {
						return t.copy(parent, args);
					}
				}
		);*/
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
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
	protected Term auxInsert(TermList list) {
		lv.insertIndices.push(insertLevel - 1);
		Term ret = copy(list, Arrays.asList(t.insertInto(list)));
		lv.insertIndices.pop();

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


	@Override
	protected boolean auxUnify(Term that) {
		// this only works if terms are in some normalform (like beta-eta NF).
		if(that instanceof Lambda) {
			Lambda l = (Lambda) that;

			lv.link = l.lv; // careful, lambda variables may have different indices.

			boolean ret = t.unify(l.t);

			// unset link that was set before
			lv.link = null;

			return ret;
		}

		return false;
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		Term copy_t = args.get(0);

		/*if(t.level != copy_t.level) {
			// in this case, modify
			// this one is important for eg \1.(\0.%0) %1

			copy_t = copy_t.replaceLambda(t.level, copy_t.level);
		}*/

		return Lambda.create(list, copy_t);
	}

	protected String str() {
		// debruijn index is by 1 too high (which is better for some algorithms!)
		return "\\" + (level - 1) + "." + t;
	}

}
