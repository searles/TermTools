import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.*;
import at.searles.terms.rules.CTRS;
import at.searles.terms.rules.RewriteRule;
import at.searles.terms.rules.TRS;

import java.util.Iterator;
import java.util.function.Function;

public class Demo {

	static void test1() {
		// interesting test whether two similar terms (same app involving only lambda terms)
		// can be untangled.

		TermList l = new TermList();

		Parser<Term> p = TermParserBuilder.HO_BUILDER.parser(l, s -> true);

		Term t1 = p.parse("\\x.(\\y.X (x y)) (\\z.(x z) Y)"); //

		System.out.println(l);

		Term t2 = p.parse("X");
		Term t3 = p.parse("Y");
		Term t4 = p.parse("\\x.S");
		Term t5 = p.parse("\\x y.S");

		System.out.println("t1 = " + t1);

		t2.link = t4;
		t3.link = t5;

		TermList l2 = new TermList();

		Term post = l2.insert(t1);

		System.out.println("post = " + post);
		System.out.println("l2 = " + l2);

		Term postInline = l.insert(t1);

		System.out.println("postInline = " + postInline);
		System.out.println("l = " + l);

	}

	static void test2() {
		TermList l1 = new TermList();
		TermList l2 = new TermList(); // replace this by l1 to see how the inner x is used then.

		Term lfx = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("\\x.F x");
		Term f = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("F");
		Term lgx = TermParserBuilder.HO_BUILDER.parser(l2, s -> true).parse("\\x.G x");
		Term g = TermParserBuilder.HO_BUILDER.parser(l2, s -> true).parse("G");
		Term l0_1 = LambdaVar.create(l1, 0);
		Term l0_2 = LambdaVar.create(l2, 0);

		f.link = lgx;

		g.link = l0_1;
		Term t1 = l1.insert(lfx);
		System.out.println(t1);

		g.link = l0_2;
		Term t2 = l1.insert(lfx);
		System.out.println(t2);
	}

	static void test3() {
		// to show that lambda-index can also be reduced.
		TermList l = new TermList();
		Term t = TermParserBuilder.HO_BUILDER.parser(l, s -> true).parse("\\x.x \\y.A y");
		Term u = TermParserBuilder.HO_BUILDER.parser(l, s -> true).parse("\\y.A y");

		u.link = TermParserBuilder.HO_BUILDER.parser(l, s -> true).parse("B");

		Term t2 = l.insert(t);

		System.out.println(t2);
	}

	static void demoRuleTest() {
		// shows matching of ho-terms. Important: All lambda terms must be in some normalform.
		TermList l1 = new TermList();
		Term lhs = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("\\y.\\x.0 x F");
		Term rhs = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("1 F ");

		TermList l2 = new TermList();
		Term t = TermParserBuilder.HO_BUILDER.parser(l2, s -> true).parse("\\x.\\z.0 z \\y.y");

		if(lhs.match(t)) {
			Term r2 = l2.insert(rhs);
			System.out.println(r2);
			lhs.unmatch();
		} else {
			System.out.println("no match!");
		}
	}


	static void demoUnifyTest() {
		// shows matching of ho-terms. Important: All lambda terms must be in some normalform.
		TermList l1 = new TermList();
		Term t1 = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("(((a z) y) x) w");
		Term t2 = TermParserBuilder.HO_BUILDER.parser(l1, s -> true).parse("w (x (y (z a))) ");

		if(t1.unify(t2)) {
			Term t3 = l1.insert(t1);
			t1.ununify();
			t2.ununify();

			System.out.println(t3);
		} else {
			System.out.println("ununifiable");
		}
	}

	static void demoNormalize1() {
		TermList l = new TermList();

		Function<String, Boolean> isVar = s -> Character.isUpperCase(s.charAt(0));

		Term t = TermParserBuilder.HO_BUILDER.parser(l, isVar).parse("power (s(s(s 0))) (s(s(s 0)))");

		TRS trs = new TRS.TRSParser(new Lexer(), isVar).parse(
				"plus X 0 -> X;" +
						"plus X (s Y) -> s (plus X Y);" +
						"times X 0 -> 0;" +
						"times X (s Y) -> plus (times X Y) X;" +
						"power X 0 -> (s 0);" +
						"power X (s Y) -> times (power X Y) X;"
		);

		Iterator<Term> i = l.iterator();

		while(i.hasNext()) {
			Term u = i.next();
			// in u there might be some links set, therefore insert it.

			// if 'link' is set in some subterm, then resolve it, otherwise rewrite it.
			boolean isLinked = false;

			for(Term v : u.dag()) {
				if(v.link != null) {
					isLinked = true;
					break;
				}
			}

			if(isLinked) {
				u.link = l.insert(u);
				// don't do anything with this term. It will be inserted in the
				// termlist (or if it existed before we have rewritten it already).
			} else {
				Term reduct = trs.apply(u, l);
				u.link = reduct;
			}
		}

		while(t.link != null) {
			t = t.link;
		}

		System.out.println("nf = " + t);
	}

	static void demoDagIterator() {
		TermList l = new TermList();

		Parser<Term> p = TermParserBuilder.HO_BUILDER.parser(l, s -> true);

		Term t1 = p.parse("\\x.(\\y.X (x y)) (\\z.(x z) Y)"); //

		for(Term t : t1.dag()) {
			System.out.println(t);
		}
	}

	static String num(int i) {
		String s = "0";
		while(i > 0) {
			s = "s(" + s + ")";
			i--;
		}

		return s;
	}

	static String math(String op, int...is) {
		String[] arr = new String[is.length];

		for(int i = 0; i < is.length; ++i) {
			arr[i] = num(is[i]);
		}

		return math(op, arr);
	}

	static String math(String op, String...args) {
		String s = op + "(";

		for(int i = 0; i < args.length; ++i) {
			if(i > 0) s += ", ";
			s += args[i];
		}

		return s + ")";
	}

	/*
	static void demoNormalize2() {
	// fixme this is a really nice idea for rewriting
		TermList l = new TermList();

		Function<String, Boolean> isVar = s -> Character.isUpperCase(s.charAt(0));

		String num = "s(s(s(0)))";

		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse(math("/", math("^", 5, 4), math("^", 5, 3)));

		Iterator<Term> i = l.iterator();

		while(i.hasNext()) {
			Term u = i.next();
			// in u there might be some links set, therefore insert it.

			// if 'link' is set in some subterm, then resolve it, otherwise rewrite it.
			boolean isLinked = false;

			for(Term v : u.dag()) {
				if(v.link != null) {
					isLinked = true;
					break;
				}
			}

			if(isLinked) {
				u.link = l.insert(u);
				// don't do anything with this term. It will be inserted in the
				// termlist (or if it existed before we have rewritten it already).
			} else {
				Term reduct = trs.apply(u, l);
				u.link = reduct;
			}
		}

		while(t.link != null) {
			t = t.link;
		}

		System.out.println("nf = " + t + ", " + l.size());
	}*/

	static void demoDagIteratorFO() {
		TermList l = new TermList();

		Parser<Term> p = TermParserBuilder.FO_BUILDER.parser(l, s -> true);

		Term t1 = p.parse("+(A,***,B, -> )"); //
		Term t2 = p.parse("^(s(s(s(0))), s(s(s(0))))");

		for(Term t : t1.dag()) {
			System.out.println(t);
		}

		System.out.println(l);

		System.out.println(t1);
		System.out.println(t2);
	}

	static void demoRuleParser() {
		Parser<RewriteRule> pr = new RewriteRule.RuleParser(new Lexer(), s -> true);

		System.out.println(pr.parse("+(a,b,*0)->^(B,HELLO,$$$)"));
	}

	static final Function<String, Boolean> isVar = s -> Character.isUpperCase(s.charAt(0));

	static void rewriteTRS(String strs, String term) {
		TRS trs = new TRS.TRSParser(new Lexer(), s -> true).parse(strs);
		TermList l = new TermList();
		Term t = TermParserBuilder.FO_BUILDER.parser(l, s -> true).parse(term);

		long time = System.currentTimeMillis();

		try {
			Term nf = TermFn.normalize(trs, t);
			System.out.println("normalform = " + nf);
		} catch(Throwable th) {
			th.printStackTrace();
		}

		long dur = System.currentTimeMillis() - time;

		System.out.printf("Duration: %.3fs\n", dur / 1000.);
	}

	static void rewriteCTRS(String sctrs, String term) {
		CTRS ctrs = new CTRS.CTRSParser(new Lexer(), s -> true).parse(sctrs);
		TermList l = new TermList();
		Term t = TermParserBuilder.FO_BUILDER.parser(l, s -> true).parse(term);

		long time = System.currentTimeMillis();

		try {
			Term nf = TermFn.normalize(ctrs, t);
			System.out.println("normalform = " + nf + ", size = " + l.size());
		} catch(Throwable th) {
			th.printStackTrace();
		}

		long dur = System.currentTimeMillis() - time;

		System.out.printf("Duration: %.3fs\n", dur / 1000.);
	}

	static void demoNormalizeCTRS() {
		String ctrs =   "+(x,0) -> x " +
						"+(x,s(y)) -> s(+(x,y)) " +
						"-(s(x),s(y)) -> -(x,y) " +
						"-(x,0) -> x " +
						"*(x,0) -> 0 " +
						"*(x,s(y)) -> +(x, *(x,y)) " +
						"^(x,0) -> s(0) " +
						"^(x,s(y)) -> *(x, ^(x, y)) " +
						"<(s(x),s(y)) -> <(x, y) " +
						"<(0, s(x)) -> true() " +
						"<(x, 0) -> false() " +
						"/(x,y) -> pair(0, x) <=  <(x,y) -> true() " +
						"/(x,y) -> pair(s(q), r) <=  <(x,y) -> false(), /(-(x,y), y) -> pair(q,r) ";

		String term = math("/", math("^", 5, 6), math("^", 5, 5));

		rewriteCTRS(ctrs, term);
	}

	static void demoNormalizeCTRSNonOpTerm1() {
		CTRS ctrs = new CTRS.CTRSParser(new Lexer(), s -> true).parse(
				"f(x) -> x <= f(f(x)) -> 0 " +
						"f(f(0)) -> 0"
		);

		TermList l = new TermList();

		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("f(f(0))");

		Term u = TermFn.normalize(ctrs, t);

		System.out.println("nf = " + u + ", size = " + l.size());

	}

	static void demoNormalizeCTRSNonOpTerm2() {
		CTRS ctrs = new CTRS.CTRSParser(new Lexer(), s -> true).parse(
				"f(x) -> pair(s(y), z) <= f(x) -> pair(y, s(z)) " +
						"f(x) -> pair(0, x) "
		);

		TermList l = new TermList();

		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("f(s(s(0)))");

		Term u = TermFn.normalize(ctrs, t);

		System.out.println("nf = " + u + ", size = " + l.size());
	}



	static void demoNormalizeTRSNonOpTerm3() {
		rewriteTRS("f(0) -> f(0) " +
				"f(1) -> 1 " +
				"0 -> 1", "f(0)");
	}

	static void demoIterator() {
		TermList l = new TermList();

		Function<String, Boolean> isVar = s -> Character.isUpperCase(s.charAt(0));
		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("f(x, g(y))");

		Term.TermIterator it = t.treeIterator();

		Term u = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("H(1,I(2))");

		Term v = null;

		int i = 0;

		while(it.hasNext()) {
			i++;
			System.out.println(it.next());
			if(i == 2) v = it.replace(u);
		}

		System.out.println(v);
		System.out.println(l);
	}


	public static void main(String...ignore) {
		demoNormalizeCTRS();
	}
}

// f x (g x)
// f (h y) (g (h 0))

// add(0,x)