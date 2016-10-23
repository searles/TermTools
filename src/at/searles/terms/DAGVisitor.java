package at.searles.terms;

import java.util.TreeMap;

/**
 *
 */
public abstract class DAGVisitor<A> implements TermVisitor<A> {
    private final TreeMap<Term, A> cache = new TreeMap<>(TermList.CMP);

    protected A get(Term t) {
        return cache.get(t);
    }

    public final A visit(Term t) {
        if (cache.containsKey(t)) {
            return cache.get(t);
        } else {
            A visited = eval(t);
            cache.put(t, visited);
            return visited;
        }
    }

    public final A visitFun(Fun f) {
        if (cache.containsKey(f)) {
            return cache.get(f);
        } else {
            A visited = evalFun(f);
            cache.put(f, visited);
            return visited;
        }
    }

    public final A visitConst(Const<?> c) {
        if (cache.containsKey(c)) {
            return cache.get(c);
        } else {
            A visited = evalConst(c);
            cache.put(c, visited);
            return visited;
        }
    }

    public final A visitVar(Var v) {
        if (cache.containsKey(v)) {
            return cache.get(v);
        } else {
            A visited = evalVar(v);
            cache.put(v, visited);
            return visited;
        }
    }

    public final A visitLambda(Lambda l) {
        if (cache.containsKey(l)) {
            return cache.get(l);
        } else {
            A visited = evalLambda(l);
            cache.put(l, visited);
            return visited;
        }
    }

    public final A visitLambdaVar(LambdaVar lv) {
        if (cache.containsKey(lv)) {
            return cache.get(lv);
        } else {
            A visited = evalLambdaVar(lv);
            cache.put(lv, visited);
            return visited;
        }
    }

    public final A visitApp(App app) {
        if (cache.containsKey(app)) {
            return cache.get(app);
        } else {
            A visited = evalApp(app);
            cache.put(app, visited);
            return visited;
        }
    }

    protected A eval(Term t) {
        throw new UnsupportedOperationException();
    }

    protected A evalFun(Fun f) {
        return eval(f);
    }

    protected A evalVar(Var v) {
        return eval(v);
    }

    protected A evalConst(Const<?> c) {
        return eval(c);
    }

    protected A evalLambda(Lambda l) {
        return eval(l);
    }

    protected A evalLambdaVar(LambdaVar lv) {
        return eval(lv);
    }

    protected A evalApp(App app) {
        return eval(app);
    }
}
