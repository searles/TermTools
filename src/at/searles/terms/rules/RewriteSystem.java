package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.Term;
import at.searles.terms.TermList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RewriteSystem {

	public static class ParserBuilder extends RewriteRule.ParserBuilder {
		final Parser<String> semicolon;

		protected ParserBuilder(Lexer l) {
			super(l);
			semicolon = l.tok(";");
		}

		public class RewriteSystemParser extends Parser<RewriteSystem> {
			RewriteRule.ParserBuilder.RuleParser rp = new RewriteRule.ParserBuilder.RuleParser();

			@Override
			public RewriteSystem parse(Buffer buf) {

				// fixme how do I put list into the ruleparser/termparser?
				return null;
			}
		}
	}

	List<Rule> rules;

	public RewriteSystem(List<Rule> rules) {
		this.rules = rules;
	}

	public Term apply(Term t, TermList target) {
		return null;
	}
}
