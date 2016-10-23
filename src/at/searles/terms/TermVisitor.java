package at.searles.terms;

import org.testng.annotations.Optional;

import java.util.TreeMap;

/**
 * Visitor-Interface
 */
public interface TermVisitor<A> {

    default A visit(Term t) {
        throw new UnsupportedOperationException();
    }

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


}
