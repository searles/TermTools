package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.*;

import java.util.function.Function;

public class RewriteRule {

	public final Term lhs;
	public final Term rhs;
	protected final TermList list;


	public RewriteRule(Term lhs, Term rhs) {
		this.list = lhs.parent;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	/**
	 * Applies this rule to t, inserting a reduct into the termqueue in target.
	 * @param t the term to be rewritten
	 * @param target if null, then a new termqueue is created.
	 * @return null if not applicable
	 */
	public Term apply(Term t, TermList target, boolean loopCheck) {
		if(lhs.match(t)) {
			Term reduct = target.insert(rhs);
			lhs.unmatch();
			return reduct;
		} else {
			return null;
		}
	}

	public String toString() {
		return lhs + " -> " + rhs;
	}

	// do I need the builder? Yes, because of lexer.
	public static class RuleParser extends Parser<RewriteRule> {

		final Parser<String> to;
		final TermParserBuilder parent;
		final Function<String, Boolean> isVar;

		public RuleParser(Lexer l, Function<String, Boolean> isVar) {
			this.isVar = isVar;
			Lexer toLexer = new Lexer();
			toLexer.addIgnore("[ \t\n]");
			this.to = toLexer.tok("->");
			parent = new TermParserBuilder.FirstOrder(l); // fixme be more flexible on the builder.
		}

		@Override
		public RewriteRule parse(Buffer buf) {
			TermList list = new TermList();

			Parser<Term> tp = parent.parser(list, isVar); // TermParser is not static and thus uses the outer lexer.

			return tp.then(to.thenRight(tp)).map(
					concat -> new RewriteRule(concat.a, concat.b)
			).parse(buf);
		}
	}

}
