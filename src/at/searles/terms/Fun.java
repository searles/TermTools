package at.searles.terms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Fun extends Term {

	public static TermList.Node create(TermList list, String f, List<TermList.Node> args) {
		// insert and find maximum
		TermList.Node max = null;

		List<Term> tArgs = new ArrayList<>();

		for(TermList.Node arg : args) {
			if(max == null || arg.index > max.index) max = arg;
			tArgs.add(arg.value);
		}

		return list.assertInList(Fun.create(f, tArgs), max);
	}

	String f;
	ArrayList<Term> args;

	public static Fun create(String f, List<Term> args) {
		int level = 0;
		int lambda = -1;
		boolean closed = true; // let's be optimistic

		for(Term arg: args) {
			if(level < arg.level) level = arg.level;
			if(lambda < arg.maxLambdaIndex) lambda = arg.maxLambdaIndex;
		}

		return new Fun(f, args, level, lambda);
	}

	private Fun(String f, List<Term> args, int level, int maxLambdaIndex) {
		super(level, maxLambdaIndex);
		this.f = f;
		this.args = new ArrayList<>(args.size());
		this.args.addAll(args);
	}

	@Override
	public Iterable<Term> subterms() {
		return args;
	}

	@Override
	void auxInitLevel(List<LambdaVar> lvs) {
		insertedClosed = true; // true if all args are insertedClosed
		insertLevel = 0; // maximum of all args

		for(Term arg : args) {
			arg.initLevel(lvs);

			if(!arg.insertedClosed) insertedClosed = false;
			if(arg.insertLevel > insertLevel) insertLevel = arg.insertLevel;
		}
	}

	@Override
	TermList.Node auxInsert(TermList list) {
		List<TermList.Node> nArgs = new ArrayList<>();

		for(Term arg : args) {
			nArgs.add(arg.insertInto(list));
		}

		return Fun.create(list, f, nArgs);
	}

	@Override
	public boolean eq(Term t) {
		return t instanceof Fun && f.equals(((Fun) t).f) && Term.checkIdenticalSubterms(args, t.subterms());
	}

	@Override
	protected boolean auxMatch(Term that) {
		if(that instanceof Fun) {
			Fun fun = (Fun) that;

			if(f.equals(fun.f) && args.size() == fun.args.size()) {
				// match subterms
				for(int i = 0; i < args.size(); ++i) {
					// this is why I use an ArrayList
					if(!args.get(i).match(fun.args.get(i))) {
						// no match in ith argument
						for(int k = 0; k < i; ++k) {
							// unmatch all up to i.
							args.get(k).unmatch();
						}

						return false;
					}
				}

				return true;
			} else {
				// clash
				return false;
			}
		} else {
			// clash
			return false;
		}
	}


	@Override
	protected boolean auxUnify(Term that) {
		if(that instanceof Fun) {
			Fun fun = (Fun) that;

			if(f.equals(fun.f) && args.size() == fun.args.size()) {
				// unify subterms
				for(int i = 0; i < args.size(); ++i) {
					// this is why I use an ArrayList
					if(!args.get(i).unify(fun.args.get(i))) {
						// no match in ith argument
						for(int k = 0; k < i; ++k) {
							// unmatch all up to i.
							args.get(k).ununify();
							fun.args.get(k).ununify();
						}

						return false;
					}
				}

				return true;
			} else {
				// clash
				return false;
			}
		} else {
			// clash
			return false;
		}
	}

	/*@Override
	public Term copy(List<Term> subterms) {
		// check for identity while we're at it.
		boolean eq = true;

		Iterator<Term> si = subterms().iterator();
		Iterator<Term> ti = subterms.iterator();

		while(si.hasNext()) {
			if(si.next() != ti.next()) {
				eq = false;
				break;
			}
		}

		return eq ? this : new Fun(f, subterms);
	}*/

	protected String str() {
		if(args.isEmpty()) {
			return f + "()";
		} else {
			StringBuilder sb = new StringBuilder(f).append("(").append(args.get(0));

			for(int i = 1; i < args.size(); ++i) {
				sb.append(", ").append(args.get(i));
			}

			return sb.append(")").toString();
		}
	}
}
