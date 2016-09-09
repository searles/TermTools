package at.searles.terms;

import java.util.LinkedList;
import java.util.List;

public class CycleException extends RuntimeException {
    List<Term> cycle = new LinkedList<>();

    public CycleException(Term t) {
        this.cycle.add(t);
    }

    public void append(Term t) {
        this.cycle.add(t);
    }

    public String toString() {
        return "Cycle: " + cycle.toString();
    }
}
