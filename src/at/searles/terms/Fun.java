package at.searles.terms;

import java.util.*;

public class Fun extends Term {

	public static Term create(TermList list, String f, List<Term> args) {
		// insert and find maximum
		Term[] tArgs = new Term[args.size()];

		int i = 0;

		for(Term arg : args) {
			tArgs[i++] = arg;
		}

		return createUnsafe(list, f, tArgs);
	}


	private static Term createUnsafe(TermList list, String f, Term[] args) {
		// insert and find maximum
		Term max = null;
		int level = 0;
		int lambda = -1;

		for(int i = 0; i < args.length; ++i) {
			Term arg = args[i];
			if(max == null || arg.index > max.index) max = arg;
			if(level < arg.level) level = arg.level;
			if(lambda < arg.maxLambdaIndex) lambda = arg.maxLambdaIndex;
		}

		return list.findOrAppend(new Fun(f, args, level, lambda), max);
	}

	/*public static Fun cons(String f, List<Term> args) {
		int level = 0;
		int lambda = -1;

		for(Term arg: args) {
			if(level < arg.level) level = arg.level;
			if(lambda < arg.maxLambdaIndex) lambda = arg.maxLambdaIndex;
		}

		ArrayList<Term> tArgs = new ArrayList<>();
		tArgs.addAll(args);

		return new Fun(f, tArgs, level, lambda);
	}*/

	String f;
	Term[] args;

	private Fun(String f, Term[] args, int level, int maxLambdaIndex) {
		super(level, maxLambdaIndex);
		this.f = f;
		this.args = args;
	}

	@Override
	public void auxInitLevel(List<LambdaVar> lvs) {
		insertedClosed = true; // true if all args are insertedClosed
		insertLevel = 0; // maximum of all args

		for(int i = 0; i < args.length; ++i) {
			Term arg = args[i];
			arg.initLevel(lvs);

			if(!arg.insertedClosed) insertedClosed = false;
			if(arg.insertLevel > insertLevel) insertLevel = arg.insertLevel;
		}
	}

	@Override
	protected Term auxInsert(TermList list) {
		Term[] nArgs = new Term[args.length];

		for(int i = 0; i < args.length; ++i) {
			nArgs[i] = args[i].insertInto(list);
		}

		return Fun.createUnsafe(list, f, nArgs);
	}

	@Override
	public int arity() {
		return args.length;
	}

	@Override
	public Term arg(int p) {
		return args[p];
	}

	@Override
	public Term replace(int p, Term t) {
		Term[] newArgs = new Term[arity()];

		for(int i = 0; i < arity(); ++i) {
			newArgs[i] = i == p ? t : args[i];
		}

		return createUnsafe(parent, f, newArgs);
	}


	@Override
	public boolean eq(Term t) {
		return t instanceof Fun && f.equals(((Fun) t).f) && Term.checkIdenticalSubterms(args, ((Fun) t).args);
	}

	@Override
	protected boolean auxMatch(Term that) {
		if(that instanceof Fun) {
			Fun fun = (Fun) that;

			if(f.equals(fun.f) && args.length == fun.args.length) {
				// match args
				for(int i = 0; i < args.length; ++i) {
					// this is why I use an ArrayList
					if(!args[i].match(fun.args[i])) {
						// no match in ith argument
						for(int k = 0; k < i; ++k) {
							// unmatch all up to i.
							args[k].unmatch();
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

			if(f.equals(fun.f) && args.length == fun.args.length) {
				// unify args
				for(int i = 0; i < args.length; ++i) {
					// this is why I use an ArrayList
					if(!args[i].unify(fun.args[i])) {
						// no match in ith argument
						for(int k = 0; k < i; ++k) {
							// unmatch all up to i.
							args[k].ununify();
							fun.args[k].ununify();
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

	protected String str() {
		if(args.length == 0) {
			return f + "()";
		} else {
			StringBuilder sb = new StringBuilder(f).append("(").append(args[0]);

			for(int i = 1; i < args.length; ++i) {
				sb.append(", ").append(args[i]);
			}

			return sb.append(")").toString();
		}
	}

	@Override
	public Term copy(TermList list, List<Term> args) {
		// this one should use an arraylist.
		int copy_level = 0;

		for(Term arg : args) {
			if(copy_level < arg.level) copy_level = arg.level;
		}

		// first, check whether it has to update this one.
		int d = copy_level - this.level;

		if(d != 0) {
			// yes, I must update some lambdas.
			ListIterator<Term> l = args.listIterator();

			while(l.hasNext()) {
				Term copy_arg = l.next();

				l.set(copy_arg.updateLambda(d));
			}
		}

		return Fun.create(list, this.f, args);
	}
}
