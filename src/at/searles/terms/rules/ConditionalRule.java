package at.searles.terms.rules;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;
import at.searles.terms.*;

import java.util.*;
import java.util.function.Function;

/**
 * Conditional Rewrite Rules
 */
public class ConditionalRule {

    final TermList list;
    final Term lhs;
    final Term rhs;
    final List<Condition> conditions;

    public ConditionalRule(Term lhs, Term rhs, List<Condition> conditions) {
        this.list = lhs.parent;
        this.lhs = lhs;
        this.rhs = rhs;
        this.conditions = conditions;
    }

    public static class Condition {
        Term s;
        Term t;

        Condition(Term s, Term t) {
            this.s = s;
            this.t = t;
        }

        public String toString() {
            return s + " -> " + t;
        }
    }

    public Term apply(Term t, TermList target, TermFn fn) {
        if(lhs.match(t)) {
            for (Condition c : conditions) {
                Term ssigma = target.insert(c.s); // matched lhs of condition

                // store matcher
                Map<Var, Term> matcher = list.matcher();
                list.clearMatcher();

                Term u = ssigma.normalize(fn);

                // restore matcher
                for (Map.Entry<Var, Term> entry : matcher.entrySet()) {
                    entry.getKey().link = entry.getValue();
                }

                // and compare normalform of ssigma with c.t
                if (!c.t.match(u)) {
                    // matcher was unset already
                    // but reset all variables
                    for (Var v : matcher.keySet()) {
                        v.link = null;
                    }

                    // condition is not satisfied.
                    return null;
                }
            }

            Term reduct = target.insert(rhs);

            list.clearMatcher();

            return reduct;
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuilder sb = null;

        for(Condition c : conditions) {
            if(sb != null) {
                sb.append(", ");
            } else {
                sb = new StringBuilder();
            }

            sb.append(c.s + " -> " + c.t);
        }

        return lhs + " -> " + rhs + (sb == null ? "" : (" <= " + sb.toString()));
    }

    /**
     * This class is a special term type that also includes the conditions.
     * Is it a good idea to write it like this? Difficult to say...
     * I once implemented it like this but it did not work well because
     * eq would involve a lot of comparisons. Feasible though...
     */
    /*public static class ConditionalTerm extends Term {

        public static Term create(TermList list, Term term, ConditionalRule rule, Map<Var, Term> matcher, int conditionIndex) {
            return null;
        }

        final ConditionalRule rule; // the rule that was applied
        final Map<Var, Term> matcher;
        final int conditionIndex;
        final Term current;

        protected ConditionalTerm(Term current, ConditionalRule rule, Map<Var, Term> matcher, int conditionIndex) {
            super(current.level, current.maxLambdaIndex);
            this.current = current;
            this.rule = rule;
            this.matcher = matcher;
            this.conditionIndex = conditionIndex;
        }

        @Override
        protected String str() {
            return "TODO";
        }

        @Override
        public void auxInitLevel(List<LambdaVar> lvs) {
            current.auxInitLevel(lvs);
        }

        @Override
        protected Term auxInsert(TermList list) {
            // fixme: What the hell??? Why can't these methods be protected???
            // TODO Complain somewhere that protected DOES NOT MEAN VISIBILITY OF SUPERCLASS's METHODs
            return new ConditionalTerm(current.insertInto(list), rule, matcher, conditionIndex);
        }

        @Override
        public Iterable<Term> args() {
            return Collections.singletonList(current);
        }

        @Override
        public Term copy(TermList list, List<Term> args) {
            return ConditionalTerm.create(list, args.get(0), rule, matcher, conditionIndex);
        }

        @Override
        protected boolean eq(Term t) {
            if(t instanceof ConditionalTerm) {
                ConditionalTerm ct = (ConditionalTerm) t;

            }
            return false;
        }

        @Override
        protected boolean auxMatch(Term that) {
            // does not unify or match.
            return false;
        }

        @Override
        protected boolean auxUnify(Term that) {
            return false;
        }

    }*/

    public static class RuleParser extends Parser<ConditionalRule> {

        final Parser<String> to;
        final Parser<String> from;
        final Parser<String> comma;
        final TermParserBuilder parent;
        final Function<String, Boolean> isVar;

        public RuleParser(Lexer l, Function<String, Boolean> isVar) {
            this.isVar = isVar;
            Lexer toLexer = new Lexer();
            toLexer.addIgnore("[ \t\n]");
            this.to = toLexer.tok("->");
            this.from = toLexer.tok("<=");
            this.comma = toLexer.tok(",");
            parent = new TermParserBuilder.FirstOrder(l); // fixme be more flexible on the builder.
        }

        @Override
        public ConditionalRule parse(Buffer buf) {
            TermList list = new TermList();

            Parser<Term> tp = parent.parser(list, isVar); // TermParser is not static and thus uses the outer lexer.

            Parser<Condition> cp = tp.then(to.thenRight(tp)).map(tt -> new Condition(tt.a, tt.b));

            Parser<List<Condition>> cs = from.thenRight(cp.then(comma.thenRight(cp).rep(true))).map(cc -> {
                List<Condition> l = new LinkedList<>();
                l.add(cc.a);
                l.addAll(cc.b.asList());
                return l;
            });

            return tp.then(to.thenRight(tp)).then(cs.opt()).map(
                    concat -> new ConditionalRule(concat.a.a, concat.a.b, concat.b.isDef ? concat.b.get() : Collections.<Condition>emptyList() )
            ).parse(buf);
        }
    }

    /*@Override
    public Term apply(Term t, TermList target) {
        if(lhs.match(t)) {
            // store matcher
            return null;
        } else {
            return null;
        }
    }*/

}
