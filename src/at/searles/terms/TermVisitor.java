package at.searles.terms;

import java.util.TreeMap;

/**
 * Visitor-Interface
 */
public interface TermVisitor<A> {

    A visit(Term t);

    default A visitFun(Fun f) {
        return visit(f);
    }

    default A visitConst(Const<?> c) {
        return visit(c);
    }

    default A visitVar(Var v)  {
        return visit(v);
    }

    default A visitLambda(Lambda l)  {
        return visit(l);
    }

    default A visitLambdaVar(LambdaVar lv)  {
        return visit(lv);
    }

    default A visitApp(App app)  {
        return visit(app);
    }

    default TermVisitor<A> toDAGVisitor() {
        return new DAGVisitor<A>(this);
    }

    class DAGVisitor<A> implements TermVisitor<A> {
        private final TermVisitor<A> parent;
            private final TreeMap<Term, A> cache = new TreeMap<>(TermList.CMP);

        public DAGVisitor(TermVisitor<A> parent) {
            this.parent = parent;
        }

        public A visit(Term t) {
            if (cache.containsKey(t)) {
                return cache.get(t);
            } else {
                return cache.put(t, parent.visit(t));
            }
        }

        public A visitFun(Fun f) {
            if (cache.containsKey(f)) {
                return cache.get(f);
            } else {
                return cache.put(f, parent.visitFun(f));
            }
        }

        public A visitConst(Const<?> c) {
            if (cache.containsKey(c)) {
                return cache.get(c);
            } else {
                return cache.put(c, parent.visitConst(c));
            }
        }

        public A visitVar(Var v) {
            if (cache.containsKey(v)) {
                return cache.get(v);
            } else {
                return cache.put(v, parent.visitVar(v));
            }
        }

        public A visitLambda(Lambda l) {
            if (cache.containsKey(l)) {
                return cache.get(l);
            } else {
                return cache.put(l, parent.visitLambda(l));
            }
        }

        public A visitLambdaVar(LambdaVar lv) {
            if (cache.containsKey(lv)) {
                return cache.get(lv);
            } else {
                return cache.put(lv, parent.visitLambdaVar(lv));
            }
        }

        public A visitApp(App app) {
            if (cache.containsKey(app)) {
                return cache.get(app);
            } else {
                return cache.put(app, parent.visitApp(app));
            }
        }
    }
}
