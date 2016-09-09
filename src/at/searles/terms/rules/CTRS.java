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

/**
 * Conditional Term Rewrite System
 */
public class CTRS implements TermFn {
    public static class CTRSParser extends Parser<CTRS> {

        public final ConditionalRule.RuleParser ruleParser;
        public final Parser<CTRS> parser;

        public CTRSParser(Lexer l, Function<String, Boolean> isVar) {
            ruleParser = new ConditionalRule.RuleParser(l, isVar);
            parser = ruleParser.rep(true).map(
                    rules -> new CTRS(rules.asList())
            );
        }

        @Override
        public CTRS parse(Buffer buf) {
            return parser.parse(buf);
        }
    }

    private final List<ConditionalRule> rules;

    public CTRS(List<ConditionalRule> rules) {
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
        for(ConditionalRule r : rules) {
            try {
                Term u = r.apply(t, target, this, false);
                if(u != null) return u;
            } catch(CycleException c) {
                ex = c;
            }
        }

        if(ex != null) throw ex; // if there was an exception and no other reduct...

        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(ConditionalRule r : rules) {
            sb.append(r.toString()).append("\n");
        }

        return sb.toString();
    }
}
