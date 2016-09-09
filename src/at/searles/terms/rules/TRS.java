package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.CycleException;
import at.searles.terms.Term;
import at.searles.terms.TermFn;
import at.searles.terms.TermList;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class TRS implements TermFn {

	public static class TRSParser extends Parser<TRS> {

		public final RewriteRule.RuleParser ruleParser;
		//public final Parser<String> semicolon;
		public final Parser<TRS> parser;

		public TRSParser(Lexer l, Function<String, Boolean> isVar) {
			ruleParser = new RewriteRule.RuleParser(l, isVar);
			//this.semicolon = l.tok("\\;");
			parser = ruleParser.rep(true).map(
					rules -> new TRS(rules.asList())
			);
		}


		@Override
		public TRS parse(Buffer buf) {
			return parser.parse(buf);
		}
	}

	private final List<RewriteRule> rules;

	public TRS(List<RewriteRule> rules) {
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
		CycleException ex = null;

		for(RewriteRule r : rules) {
			try {
				Term u = r.apply(t, target, false);
				if (u != null) {
					return u;
				}
			} catch(CycleException c) {
				// If a rule leads to a cycle, try another rule. Maybe we will find a normalform.
				ex = c;
			}
		}

		// there were only cycles?
		if(ex != null) throw ex;

		// No, but the term could not be reduced, thus return null.
		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(RewriteRule r : rules) {
			sb.append(r).append("\n");
		}

		return sb.toString();
	}
}
