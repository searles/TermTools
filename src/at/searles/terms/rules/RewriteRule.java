package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.Term;
import at.searles.terms.TermList;
import at.searles.terms.TermParserBuilder;

public class RewriteRule implements Rule {
	// do I need the builder? Yes, because of lexer.
	public static class ParserBuilder extends TermParserBuilder {

		public static final ParserBuilder RULEBUILDER = new ParserBuilder(new Lexer());

		final Parser<String> to;

		protected ParserBuilder(Lexer l) {
			super(l);
			this.to = l.tok("->");
		}

		public Parser<Rule> ruleParser(TermList list) {
			return new RuleParser();
		}

		protected class RuleParser extends Parser<Rule> {
			@Override
			public Rule parse(Buffer buf) {
				TermList list = new TermList();

				TermParser tp = new TermParser(list); // TermParser is not static and thus uses the outer lexer.

				return tp.expr.then(to.thenRight(tp.expr)).map(
						concat -> new RewriteRule(list, concat.a.value, concat.b.value)
				).parse(buf);
			}
		}
	}

	// fixme maybe the next ones should be Term?
	final Term l;
	final Term r;

	private RewriteRule(TermList list, Term l, Term r) {
		this.l = l;
		this.r = r;
	}

	/**
	 * Applies this rule to t, inserting a reduct into the termqueue in target.
	 * @param t the term to be rewritten
	 * @param target if null, then a new termqueue is created.
	 * @return null if not applicable
	 */
	public Term apply(Term t, TermList target) {
		if(l.match(t)) {
			Term reduct = target.insert(r);
			l.unmatch();
			return reduct;
		} else {
			return null;
		}
	}
}
