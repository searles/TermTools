package at.searles.terms

import at.searles.parsing.parser.Parser
import at.searles.parsing.regex.Lexer
import at.searles.terms.rules.CTRS

import java.util.function.Function

/**
 * Some test cases
 */
class TermListTest extends GroovyTestCase {
    static final Function<String, Boolean> isVar = new Function<String, Boolean>() {
        @Override
        Boolean apply(String s) {
            return true;
        }
    };

    static final Function<String, Boolean> isUpperVar = new Function<String, Boolean>() {
        @Override
        Boolean apply(String s) {
            return Character.isUpperCase(s.charAt(0));
        }
    };

    static Term ho(String s, TermList l) {
        if(l == null) l = new TermList();
        Parser<Term> p = TermParserBuilder.HO_BUILDER.parser(l, isVar);
        return p.parse(s);
    }

    void testBeta0() {
        // normal beta reduction
        Term test1 = ho("(\\x.x 0) (\\y.y)", null);
        Term r = betaFn.apply(test1, test1.parent);

        assert r.toString().equals("(\\a.a) 0");

        r = test1.normalize(betaFn);
        assert r.toString().equals("0");
    }

    void testBeta1() {
        // testing whether inserting a lambda variable in a beta reduction causes troubles.
        Term t = ho("\\a.(\\b.(b a)) A", null); // the 0 is inside

        Term u = t.normalize(betaFn);

        assert u.toString().equals("\\a.A a");
    }

    void testBeta2() {
        Term t = ho("\\a.B ((\\b.b) a)", null);
        Term r = t.normalize(betaFn);
        assert r.toString().equals("\\a.B a");
    }

    void testBetaArithmetics() {
        // and some arithmetics
        TermList l = new TermList();

        Term n0 = ho("\\f.\\x.x", l);
        Term succ = ho("\\n.\\f.\\x.f (n f x)", l);

        Term r = App.create(l, succ, n0);
        r = r.normalize(betaFn);

        assert r.toString().equals("\\a.\\b.a b");

        Term t2 = ho("\\f.\\x.f ((\\f.\\x.x) f x)", new TermList());

        t2 = t2.normalize(betaFn);

        assert t2.toString().equals("\\a.\\b.a b");

        System.out.println(t2);

        assert r.toString().equals("\\a.\\b.a b");

        Term n2 = ho("\\f.\\x.f (f x)", l);
        Term n3 = ho("\\f.\\x.f (f (f x))", l);
        Term n4 = ho("\\f.\\x.f (f (f (f x)))", l);

        Term n7 = ho("\\f.\\x.f (f (f (f (f (f (f x))))))", l);

        Term plus = ho("\\m.\\n.\\f.\\x.m f (n f x)", l);
        Term add34 = App.create(l, App.create(l, plus, n3), n4).normalize(betaFn);

        assert n7 == add34;

        System.out.println(n7);

        Term mul = ho("\\m.\\n.\\f.m (n f)", l);
        Term mul37 = App.create(l, App.create(l, mul, n3), n7);

        Term n21 = mul37.normalize(betaFn);

        Term mul21_4 = App.create(l, App.create(l, mul, n21), n4);

        Term n84 = mul21_4.normalize(betaFn);

        Term pow = ho("\\b.\\e.e b", l);
        Term pow34 = App.create(l, App.create(l, pow, n3), n4);

        Term n81 = pow34.normalize(betaFn);

        Term add_81_3 = App.create(l, App.create(l, plus, n81), n3);

        Term n84_2 = add_81_3.normalize(betaFn);

        assert n84 == n84_2;
    }

    void testInsert() {
        // part 2:
        // This test shows some ambiguity in connection with
        // link and links to lambda vars.
        // No way to avoid it but need to keep it consistent.
        TermList l1 = new TermList();
        TermList l2 = new TermList(); // replace this by l1 to see how the inner x is used then.

        Term lfx = ho("\\x.F x", l1);
        Term f = ho("F", l1);
        Term l0_1 = LambdaVar.create(l1, 0, l1);

        Term lgx = ho("\\x.G x", l2);
        Term g = ho("G", l2);
        Term l0_2 = LambdaVar.create(l2, 0, l2);

        f.link = lgx; // F -> \\x.G x
        g.link = l0_1; // G -> lambda var in l1

        Term t1 = l1.insert(lfx);

        assert t1.toString().equals("\\a.(\\b.a b) a");

        g.link = l0_2;  // G -> lambda var in l1
        Term t2 = l1.insert(lfx);

        assert t2.toString().equals("\\a.(\\b.b b) a");
    }


    void testInsert2() {
        // part 1:
        TermList l = new TermList();
        Term t1 = ho("\\x.(\\y.X (x y)) (\\z.(x z) Y)", l); //

        Term t2 = ho("X", l);
        Term t3 = ho("Y", l);
        Term t4 = ho("\\x.S", l);
        Term t5 = ho("\\x y.S", l);

        // this test is interesting to check the behavior of inserting it.
        t2.link = t4;
        t3.link = t5;

        TermList l2 = new TermList();

        Term post = l2.insert(t1);

        // expected result
        Term u = ho("\\a.(\\b.(\\c.S) (a b)) (\\b.a b (\\c.\\d.S))", l2); // expected result

        assert u == post;
        assert u.toString().equals("\\a.(\\b.(\\c.S) (a b)) (\\b.a b (\\c.\\d.S))");
    }



    void testUpdateLambda3() {
        // test multiple applications of copy.
        TermList l = new TermList();
        Term t1 = ho("\\x.(box x) x", l);

        Term t2 = ho("box", l);
        Term t3 = ho("\\x y.S", l);

        // t2 should be replaced by t4 and t3 should be replaced by t5.

        // I use innermostOnDag.

        def visitor = new DAGVisitor<Term>() {
            ArrayList<Term> args = new ArrayList<>();
            @Override
            protected Term eval(Term t) {
                if(t == t2) return t3;
                else {
                    for(int i = 0; i < t.arity(); ++i) {
                        t.arg(i).visit(this);
                    }

                    args.clear();
                    args.ensureCapacity(t.arity());

                    for(int i = 0; i < t.arity(); ++i) {
                        args.add(get(t.arg(i)));
                    }

                    return t.copy(t.parent, args);
                }
            }
        };

        Term post = t1.visit(visitor);

        System.out.println(t1);
        System.out.println(post);

        assert post.toString().equals("\\a.(\\b.\\c.S) a a");
    }

    void testUpdateLambda2() {
        // another test in connection with updatelambda, now with parser.
        // it is equivalent to testInsert, only instead of link it uses copy.
        TermList l = new TermList();
        Term t1 = ho("\\x.(\\y.X (x y)) (\\z.(x z) Y)", l);

        System.out.println(t1);

        // Problem: This one fails and it does so for a good reason:
        // If copy is applied on a subterm and on a superterm, the
        // lambda variables are updated two times. Or are they?

        // fixme!

        Term t2 = ho("X", l);
        Term t3 = ho("Y", l);
        Term t4 = ho("\\x.S", l);
        Term t5 = ho("\\x y.S", l);

        // t2 should be replaced by t4 and t3 should be replaced by t5.


        def visitor = new DAGVisitor<Term>() {
            ArrayList<Term> args = null;

            @Override
            protected Term eval(Term t) {
                if (t == t2) return t4;
                else if (t == t3) return t5;
                else {
                    // visit all subnodes
                    for (int i = 0; i < t.arity(); ++i) {
                        t.arg(i).visit(this);
                    }

                    // now all subnodes were visited, so collect them
                    if (args == null && t.arity() != null) {
                        args = new ArrayList<>(t.arity());
                    } else {
                        args.clear();
                        args.ensureCapacity(t.arity());
                    }

                    for (int i = 0; i < t.arity(); ++i) {
                        args.add(get(t.arg(i)))
                    }

                    return t.copy(t.parent, args);
                }
            }
        };

        Term post = t1.visit(visitor)

        // expected result
        Term u = ho("\\x.(\\y.(\\z.S) (x y)) (\\z.(x z) (\\z.\\w.S))", l); // expected result

        assert u == post;
        assert u.toString().equals("\\a.(\\b.(\\c.S) (a b)) (\\b.a b (\\c.\\d.S))");
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

    static Term rewriteCTRS(String sctrs, String term) {
        CTRS ctrs = new CTRS.CTRSParser(new Lexer(), isVar).parse(sctrs);
        TermList l = new TermList();
        Term t = TermParserBuilder.FO_BUILDER.parser(l, isVar).parse(term);

        long time = System.currentTimeMillis();

        try {
            Term nf = t.normalize(ctrs);
            System.out.println("normalform = " + nf + ", size = " + l.size());
            return nf;
        } finally {
            double dur = System.currentTimeMillis() - time;
            System.out.printf("Duration: %.3fs\n", dur / 1000.0);
        }
    }

    void testNormalizeCTRS() {
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
                "/(x,y) -> pair(s(q), r) <= <(x,y) -> false(), /(-(x,y), y) -> pair(q,r) ";

        // how cool is that, 6^6/6^5 in 107 seconds...
        // 5^6 / 5^5 takes 10 seconds (before changing to relative debruijn)
        // after changing to relative debruijn with insertInto using innermostOnDag it is 19 seconds.
        // With the inserted field it is down to 8 seconds.
        // For 6^6 / 6^5 it is even only 76 seconds.
        String term = math("/", math("^", 5, 6), math("^", 5, 5));

        Term nf = rewriteCTRS(ctrs, term);

        Term q = nf.arg(0);
        assert q.toString().equals("s(s(s(s(s(0())))))");
    }

    static final TermFn betaFn = new TermFn() {
        @Override
        Term apply(Term t, TermList target) {
            if(t instanceof App && t.arg(0) instanceof Lambda) {
                return ((Lambda) t.arg(0)).beta(t.arg(1));
            } else {
                return null;
            }
        }
    };

    void testLambdaRewrite() {
        // Simulates Application of the rule 9 X -> \\y.y X
        // Expected result: \\x.(9 x) --> \\x.(\\y.y x)

        TermList l = new TermList();
        TermList l2 = new TermList();

        Term lhs = ho("9 X", l);
        Term rhs = ho("\\y.y X", l);

        Term t2 = ho("\\x.(9 x)", l2);

        Term subterm = t2.arg(0); // (9 x)

        assert lhs.match(subterm);

        Term rewrittenSubterm = l2.insert(rhs);

        Term rewritten = t2.copy(l2, Arrays.asList(rewrittenSubterm));

        assert rewritten.toString().equals("\\a.\\b.b a");
    }


    void testBetaLambda() {
        // testing whether inserting a lambda variable in a beta reduction causes troubles.
        Term t = ho("\\x.(\\y.y) x", null);
        Term t2 = ho("\\x.(\\y.y 0) x", null);

        Term u = t.normalize(betaFn);
        Term u2 = t2.normalize(betaFn);

        assert u.toString().equals("\\a.a");
        assert u2.toString().equals("\\a.a 0");
    }


    void testBetaCycles() {
        // are cycles found?

        Term cyclic = ho("(\\x.x x) (\\x.x x)", null);

        Term reduced = betaFn.apply(cyclic, cyclic.parent);

        assert reduced == cyclic;

        try {
            cyclic.normalize(betaFn);
            assert false;
        } catch(CycleException c) {
            assert true;
        }

        // next test
        Term cyclic2 = ho("(\\x.x x x) (\\x.x x x)", null);
        Term reduced2 = betaFn.apply(cyclic2, cyclic2.parent);

        System.out.println(reduced2);

        try {
            cyclic2.normalize(betaFn);
            assert false;
        } catch(CycleException c) {
            assert true;
        }

    }

    void testCyclicRewriting() {
        try {
            Term nf = rewriteCTRS("f(x) -> f(x)", "f(x)");
            assert false;
        } catch(CycleException ignored) {}

        try {
            Term nf = rewriteCTRS("f(x) -> f(f(x))", "f(x)");
            assert false;
        } catch(CycleException ignored) {}

        try {
            Term nf = rewriteCTRS("f(x) -> z <= f(x) -> z", "f(x)");
            assert false;
        } catch(CycleException ignored) {}

        try {
            Term nf = rewriteCTRS("f(x) -> z <= f(f(x)) -> z", "f(x)");
            assert false;
        } catch(CycleException ignored) {}

        try {
            Term nf = rewriteCTRS("a() -> b() b() -> a()", "a()");
            assert false;
        } catch(CycleException ignored) {}

        // final test: TRS contains a check that tries other rules if one lead to a cycle
        // and only if in this case there is no rule without a cycle, only then the exception is thrown.
        try {
            Term nf = rewriteCTRS("f(x) -> z <= f(x) -> z  f(x) -> a", "f(x)");
            assert true;
        } catch(CycleException ignored) {
            assert false;
        }

    }

    void testLinearize() {
        Iterator<String> varStream = new Iterator<String>() {
            int i = 0;

            @Override
            boolean hasNext() {
                return true
            }

            @Override
            String next() {
                return "x" + (i++);
            }
        }

        // function for linearization
        TermFn lin = new TermFn() {
            public Term apply(Term t, TermList target) {
                if (t instanceof Var) {
                    return Var.create(t.parent, varStream.next())
                } else {
                    return null
                }
            }
        };

        Term t = ho("\\X.f[X,Y,Z,g[X,Y]]", new TermList());

        System.out.println(t);

        Term u = t.visit(new TermVisitor<Term>() {
            int counter = 0;

            @Override
            public Term visit(Term term) {
                ArrayList<Term> args = term.arity() > 0 ? new ArrayList<>(term.arity()) : null;

                for(int i = 0; i < term.arity(); ++i) {
                    args.add(term.arg(i).visit(this));
                }

                return term.copy(term.parent, args);
            }

            @Override
            public Term visitVar(Var v) {
                String id = "x" + counter;
                counter++;
                return Var.create(v.parent, id);
            }
        });

        assert u.toString().equals("\\a.f(a, x0, x1, g(a, x2))");
    }

    void testCon() {
        // replaces all f-rooted terms by a bot.
        TermFn con = new TermFn() {
            public Term apply(Term t, TermList target) {
                if (t instanceof Fun && (((Fun) t).f.equals("f"))) {
                    return Const.create(target, "bot");
                } else {
                    return null
                }
            }
        };

        Term t = ho("\\X.g[X,f[A,Y],Z,h[X,f[Y,Z]]]", new TermList());

        System.out.println(t);

        Term u = TermFn.subtermDag(con, t, t.parent);

        System.out.println(u);

        assert u.toString().equals("\\a.g(a, bot, Z, h(a, bot))");
    }


}
