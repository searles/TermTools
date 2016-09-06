package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.Term;
import at.searles.terms.TermList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RewriteSystem {

	public static class TRSParser extends Parser<RewriteSystem> {

		public final RewriteRule.RuleParser ruleParser;
		public final Parser<String> semicolon;
		public final Parser<RewriteSystem> parser;

		public TRSParser(Lexer l, Function<String, Boolean> isVar) {
			ruleParser = new RewriteRule.RuleParser(l, isVar);
			this.semicolon = l.tok("\\;");
			parser = ruleParser.thenLeft(semicolon.opt()).rep(true).map(
					rules -> new RewriteSystem(rules.asList())
			);
		}


		@Override
		public RewriteSystem parse(Buffer buf) {
			return parser.parse(buf);
		}
	}

	private final List<Rule> rules;

	public RewriteSystem(List<Rule> rules) {
		this.rules = new LinkedList<>();
		this.rules.addAll(rules);
	}

	/**
	 * Applies this rewrite system to the term t.
	 * @param t The term to be reduced
	 * @param target The termlist into which the result is inserted.
     * @return The reduct (first one found) or null if the term is irreducible.
     */
	public Term apply(Term t, TermList target) {
		// reduces root of t by one step. Result is put into target.
		// If t cannot be reduced
		for(Rule r : rules) {
			Term u = r.apply(t, target);
			if(u != null) return u;
		}

		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(Rule r : rules) {
			sb.append(r).append("; ");
		}

		return sb.toString();
	}
}
