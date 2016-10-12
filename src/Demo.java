import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.*;
import at.searles.terms.rules.CTRS;
import at.searles.terms.rules.RewriteRule;
import at.searles.terms.rules.TRS;

import java.util.Iterator;
import java.util.function.Function;

public class Demo {


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
			Term nf = t.normalize(trs);
			System.out.println("normalform = " + nf);
		} catch(Throwable th) {
			th.printStackTrace();
		}

		long dur = System.currentTimeMillis() - time;

		System.out.printf("Duration: %.3fs\n", dur / 1000.);
	}



	static void demoNormalizeCTRSNonOpTerm1() {
		CTRS ctrs = new CTRS.CTRSParser(new Lexer(), s -> true).parse(
				"f(x) -> x <= f(f(x)) -> 0 " +
						"f(f(0)) -> 0"
		);

		TermList l = new TermList();

		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("f(f(0))");

		Term u = t.normalize(ctrs);

		System.out.println("nf = " + u + ", size = " + l.size());

	}

	static void demoNormalizeCTRSNonOpTerm2() {
		CTRS ctrs = new CTRS.CTRSParser(new Lexer(), s -> true).parse(
				"f(x) -> pair(s(y), z) <= f(x) -> pair(y, s(z)) " +
						"f(x) -> pair(0, x) "
		);

		TermList l = new TermList();

		Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse("f(s(s(0)))");

		Term u = t.normalize(ctrs);

		System.out.println("nf = " + u + ", size = " + l.size());
	}



	static void demoNormalizeTRSNonOpTerm3() {
		rewriteTRS("f(0) -> f(0) " +
				"f(1) -> 1 " +
				"0 -> 1", "f(0)");
	}

	public static void main(String...ignore) {
	}
}

// f x (g x)
// f (h y) (g (h 0))

// add(0,x)