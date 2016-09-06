import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.LambdaVar;
import at.searles.terms.Term;
import at.searles.terms.TermList;
import at.searles.terms.TermParserBuilder;
import at.searles.terms.rules.RewriteRule;
import at.searles.terms.rules.RewriteSystem;
import at.searles.terms.rules.Rule;

import java.util.Iterator;
import java.util.Scanner;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class Demo {

	static void test1() {
		// interesting test whether two similar terms (same app involving only lambda terms)
		// can be untangled.

		TermList l = new TermList();

		Parser<Term> p = TermParserBuilder.BUILDER.parser(l, s -> true);

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

		Term lfx = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("\\x.F x");
		Term f = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("F");
		Term lgx = TermParserBuilder.BUILDER.parser(l2, s -> true).parse("\\x.G x");
		Term g = TermParserBuilder.BUILDER.parser(l2, s -> true).parse("G");
		Term l0_1 = LambdaVar.create(l1, 0).value;
		Term l0_2 = LambdaVar.create(l2, 0).value;

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
		Term t = TermParserBuilder.BUILDER.parser(l, s -> true).parse("\\x.x \\y.A y");
		Term u = TermParserBuilder.BUILDER.parser(l, s -> true).parse("\\y.A y");

		u.link = TermParserBuilder.BUILDER.parser(l, s -> true).parse("B");

		Term t2 = l.insert(t);

		System.out.println(t2);
	}

	static void demoRuleTest() {
		// shows matching of ho-terms. Important: All lambda terms must be in some normalform.
		TermList l1 = new TermList();
		Term lhs = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("\\y.\\x.0 x F");
		Term rhs = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("1 F ");

		TermList l2 = new TermList();
		Term t = TermParserBuilder.BUILDER.parser(l2, s -> true).parse("\\x.\\z.0 z \\y.y");

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
		Term t1 = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("(((a z) y) x) w");
		Term t2 = TermParserBuilder.BUILDER.parser(l1, s -> true).parse("w (x (y (z a))) ");

		if(t1.unify(t2)) {
			Term t3 = l1.insert(t1);
			t1.ununify();
			t2.ununify();

			System.out.println(t3);
		} else {
			System.out.println("ununifiable");
		}
	}

	static void demoNormalize() {
		TermList l = new TermList();

		Function<String, Boolean> isVar = s -> Character.isUpperCase(s.charAt(0));

		Term t = TermParserBuilder.BUILDER.parser(l, isVar).parse("power (s(s(s 0))) (s(s(s 0)))");

		RewriteSystem trs = new RewriteSystem.TRSParser(new Lexer(), isVar).parse(
				"plus X 0 -> X;" +
						"plus X (s Y) -> plus (s X) Y;" +
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
			Term resolved = l.insert(u);

			// FIXME useful method: iterate all subterms (either using tree with positions or dag using a priority queue)

			if(resolved == u) {
				// now, no link-fields are set in u.

				Term reduct = trs.apply(u, l);
				u.link = reduct;
			}
		}

		while(t.link != null) {
			t = t.link;
		}

		System.out.println("nf = " + t);
	}

	public static void main(String...ignore) {
		demoNormalize();
	}
}

// f x (g x)
// f (h y) (g (h 0))

// add(0,x)