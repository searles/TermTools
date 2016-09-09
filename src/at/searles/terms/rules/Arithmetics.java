package at.searles.terms.rules;

public class Arithmetics {

	/*static final List<Rule> rules =
			Arrays.asList(FnRule.fromString("add(x, y)", (FnRule.Fn) (map, target) -> {
						Object x = map.get("x"); Object y = map.get("y");
						if (x instanceof Integer && y instanceof Integer) {
							return TermNode.append(target, new Const<>((Integer) x + (Integer) y));
						} else {
							return null;
						}
					}), FnRule.fromString("sub(x, y)", (FnRule.Fn) (map, target) -> {
						Object x = map.get("x"); Object y = map.get("y");
						if (x instanceof Integer && y instanceof Integer) {
							return TermNode.append(target, new Const<>((Integer) x - (Integer) y));
						} else {
							return null;
						}
					}), FnRule.fromString("mul(x, y)", (FnRule.Fn) (map, target) -> {
						Object x = map.get("x"); Object y = map.get("y");
						if (x instanceof Integer && y instanceof Integer) {
							return TermNode.append(target, new Const<>((Integer) x * (Integer) y));
						} else {
							return null;
						}
					}), FnRule.fromString("div(x, y)", (FnRule.Fn) (map, target) -> {
						Object x = map.get("x"); Object y = map.get("y");
						if (x instanceof Integer && y instanceof Integer) {
							return TermNode.append(target, new Const<>((Integer) x / (Integer) y));
						} else {
							return null;
						}
					}), FnRule.fromString("neg(x)", (FnRule.Fn) (map, target) -> {
						Object x = map.get("x");
						if (x instanceof Integer) {
							return TermNode.append(target, new Const<>(-(Integer) x));
						} else {
							return null;
						}
					}),

					// further number simplifications:
					// numbers must be propagated to top and front.
					// mul(x, y) -> mul(y, x) if y is at.searles.terms.Const and x is not.
					// mul(x, mul(y, z)) -> mul(mul(x, y), z) if y is at.searles.terms.Const
					RewriteRule.fromString("add(x, y) -> add(y, x)").membership(
							sigma -> sigma.get("y") instanceof Const && !(sigma.get("x") instanceof Const)
					),
					RewriteRule.fromString("sub(x, y) -> add(neg(y), x)").membership(
							sigma -> sigma.get("y") instanceof Const && !(sigma.get("x") instanceof Const)
					),

					RewriteRule.fromString("add(x, add(y, z)) -> add(add(y, x), z)").membership(
							sigma -> sigma.get("y") instanceof Const
					),

					RewriteRule.fromString("sub(x, add(y, z)) -> sub(add(y, x), z)").membership(
							sigma -> sigma.get("y") instanceof Const // fixme is that right???
					),

					RewriteRule.fromString("add(add(x, y), z) -> add(x, add(y, z))").membership(
							sigma -> !(sigma.get("y") instanceof Const)
					),

					RewriteRule.fromString("mul(x, y) -> mul(y, x)").membership(
							sigma -> sigma.get("y") instanceof Const && !(sigma.get("x") instanceof Const)
					),
					RewriteRule.fromString("div(x, y) -> mul(rec(y), x)").membership(
							sigma -> sigma.get("y") instanceof Const && !(sigma.get("x") instanceof Const)
					),
					RewriteRule.fromString("mul(x, mul(y, z)) -> mul(mul(x, y), z)").membership(
							sigma -> sigma.get("y") instanceof Const
					),
					RewriteRule.fromString("mul(mul(x, y), z) -> mul(x, mul(y, z))").membership(
							sigma -> !(sigma.get("y") instanceof Const)
					),

					RewriteRule.fromString("add(x, 0) -> x"),
					RewriteRule.fromString("add(0, y) -> y"),
					RewriteRule.fromString("sub(x, 0) -> x"),
					RewriteRule.fromString("sub(0, y) -> neg(y)"),

					RewriteRule.fromString("add(x, neg(y)) -> sub(x, y)"), // convert negs to subs
					RewriteRule.fromString("add(neg(x), y) -> sub(y, x)"),
					RewriteRule.fromString("sub(x, neg(y)) -> add(x, y)"),
					RewriteRule.fromString("neg(sub(x, y)) -> sub(y, x)"),

					RewriteRule.fromString("sub(neg(x), y) -> neg(add(x, y))"), // propagate negs outside
					RewriteRule.fromString("neg(neg(x)) -> x"), // remove double negs

					RewriteRule.fromString("add(sub(x, y), z) -> sub(add(x, z), y)"), // propagate sub outside
					RewriteRule.fromString("add(x, sub(y, z)) -> sub(add(x, y), z)"),

					RewriteRule.fromString("sub(sub(x, y), z) -> sub(x, add(y, z))"),
					RewriteRule.fromString("sub(x, sub(y, z)) -> sub(add(x, z), y)"),


					// multiplication rules
					RewriteRule.fromString("mul(x, 0) -> 0"),
					RewriteRule.fromString("mul(0, y) -> 0"),
					RewriteRule.fromString("div(0, y) -> 0"), // div(x, 0) is kept because it is an error

					RewriteRule.fromString("mul(x, 1) -> x"),
					RewriteRule.fromString("mul(1, y) -> y"),
					RewriteRule.fromString("div(x, 1) -> x"),
					RewriteRule.fromString("div(1, y) -> rec(y)"),

					// since neg is propagated to outside, numbers must be put on top.

					// propagate neg to outside
					RewriteRule.fromString("mul(neg(x), y) -> neg(mul(x, y))"),
					RewriteRule.fromString("mul(x, neg(y)) -> neg(mul(x, y))"),
					RewriteRule.fromString("div(neg(x), y) -> neg(div(x, y))"),
					RewriteRule.fromString("div(x, neg(y)) -> neg(div(x, y))"),
					RewriteRule.fromString("rec(neg(x)) -> neg(rec(x))"),

					RewriteRule.fromString("mul(x, rec(y)) -> div(x, y)"), // convert recs to divs
					RewriteRule.fromString("mul(rec(x), y) -> div(y, x)"),
					RewriteRule.fromString("div(x, rec(y)) -> mul(x, y)"),
					RewriteRule.fromString("rec(div(x, y)) -> div(y, x)"),

					RewriteRule.fromString("div(rec(x), y) -> rec(mul(x, y))"), // propagate recs outside
					RewriteRule.fromString("rec(rec(x)) -> x"), // remove double recs

					RewriteRule.fromString("mul(div(x, y), z) -> div(mul(x, z), y)"), // propagate div outside
					RewriteRule.fromString("mul(x, div(y, z)) -> div(mul(x, y), z)"),

					RewriteRule.fromString("div(div(x, y), z) -> div(x, mul(y, z))"),
					RewriteRule.fromString("div(x, div(y, z)) -> div(mul(x, z), y)")
			);

	// (x * 2) * (4 * y) -->

	static final TRS sys = new TRS(rules);

	public static TermNode solve(TermNode t) {
		return sys.apply(t, new TreeMap<>());
	}*/
}
