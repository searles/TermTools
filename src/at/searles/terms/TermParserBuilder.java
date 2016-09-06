package at.searles.terms;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Class to return a parser object that then can be used to parse higher order expressions.
 * The grammar is simply the default grammar of lambda calculus plus support for integers.
 */
public class TermParserBuilder {

    public static final TermParserBuilder BUILDER = new TermParserBuilder(new Lexer()); // Singleton

	private final Lexer l;

    public final Parser<String> openpar;
	public final Parser<String> closepar;
	public final Parser<String> dot;
	public final Parser<String> backslash;
	public final Parser<String> id;
	public final Parser<String> num;

    /**
     * Creates a new TermParserBuilder. Another lexer can be passed as an argument.
     * @param l possibly pre-initialized lexer. Be careful, because certain tokens are needed. Check code of
	 *          constructor to see which ones ( '(', ')', '.', '\', ids, nums.
     */
	public TermParserBuilder(Lexer l) {
        this.l = l;

        l.addIgnore(" ");
        openpar = l.tok("\\(");
        closepar = l.tok("\\)");
        dot = l.tok("\\.");
        backslash = l.tok("\\\\");
        id = l.match("[A-Za-z_][0-9A-Za-z_]*");
        num = l.match("[0-9]+");
    }

    public Parser<Term> parser(TermList list, Function<String, Boolean> isVar) {
        return new TermParser(list, isVar);
    }

    /**
     * Parser for terms. It uses the lexer from the outside builder.
     */
	public class TermParser extends Parser<Term> {

		final TermList list;
		public final Parser.PostInit<TermList.Node> expr;
		final Function<String, Boolean> isVar;

		/**
		 * Constructor. It initializes a bunch of parsers
		 * @param list list to which the parsed terms should be added.
         */
		public TermParser(TermList list, Function<String, Boolean> isVar) {
			this.list = list;
			this.isVar = isVar;

			this.expr = new Parser.PostInit<>();

			// Now for the parsers
			Parser<TermList.Node> integer = num.map(i -> Const.create(list, i));

			Parser<TermList.Node> var = id.map(s -> isVar.apply(s) ? Var.create(list, s) : Const.create(list, s));

			/**
			 * lambda = '\' id+ '.' expr
			 * <p>
			 * similar to dangeling else, exprs are always innermost
			 * <p>
			 * after variables are read, insert them into bindings
			 * so that the correct debruijn index is used. They are removed from
			 * bindings in the end.
			 */
			Parser<TermList.Node> lambda = backslash.thenRight(id.rep(false)).thenLeft(dot).then(expr).map((concat -> {
				TermList.Node n = concat.b;

				Iterator<String> it = concat.a.revIterator();

				// set links in bound variables
				while(it.hasNext()) {
					TermList.Node v = Var.create(list, it.next());

					v.value.link = LambdaVar.create(list, n.value.level).value;
					n = Lambda.create(list, list.insertNode(n.value));
					v.value.link = null;
				}

				return n;
			}));

			/**
			 * term = integer | real | var | '(' expr ')' | lambda
			 */
			Parser<TermList.Node> term =
					openpar.thenRight(expr).thenLeft(closepar)
							.or(integer)
							.or(var)
							.or(lambda);

			/**
			 * apps = term+
			 */
			Parser<TermList.Node> apps = term.rep(false).map((nodes) -> {
				TermList.Node t = null;

				for (TermList.Node n : nodes) {
					t = t == null ? n : App.create(list, t, n);
				}

				return t;
			});

			expr.set(apps);
		}

        @Override
        public Term parse(Buffer buf) {
			// parse only builds up the tree.
			// it is later inserted into the list
			Term t = expr.parse(buf).value;
            return t;
        }
    }
}
