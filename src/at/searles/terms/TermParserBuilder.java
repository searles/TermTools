package at.searles.terms;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.regex.Lexer;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Class to return a parser object that then can be used to parse higher order expressions.
 * The grammar is simply the default grammar of lambda calculus plus support for integers.
 */
public interface TermParserBuilder {
	public static final TermParserBuilder HO_BUILDER = new HigherOrder(new Lexer()); // Singleton
	public static final TermParserBuilder FO_BUILDER = new FirstOrder(new Lexer()); // Singleton

	Parser<Term> parser(TermList list, Function<String, Boolean> isVar);

	public class HigherOrder implements TermParserBuilder {

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
		public HigherOrder(Lexer l) {
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
			public final Parser.PostInit<Term> expr;
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
				Parser<Term> integer = num.map(i -> Const.create(list, i));

				Parser<Term> var = id.map(s -> isVar.apply(s) ? Var.create(list, s) : Const.create(list, s));

				/**
				 * lambda = '\' id+ '.' expr
				 * <p>
				 * similar to dangeling else, exprs are always innermost
				 * <p>
				 * after variables are read, insert them into bindings
				 * so that the correct debruijn index is used. They are removed from
				 * bindings in the end.
				 */
				Parser<Term> lambda = backslash.thenRight(id.rep(false)).thenLeft(dot).then(expr).map((concat -> {
					Term n = concat.b;

					Iterator<String> it = concat.a.revIterator();

					// set links in bound variables
					while(it.hasNext()) {
						Term v = Var.create(list, it.next());

						v.link = LambdaVar.create(list, n.level);
						n = Lambda.create(list, list.insert(n));
						v.link = null;
					}

					return n;
				}));

				/**
				 * term = integer | real | var | '(' expr ')' | lambda
				 */
				Parser<Term> term =
						openpar.thenRight(expr).thenLeft(closepar)
								.or(integer)
								.or(var)
								.or(lambda);

				/**
				 * apps = term+
				 */
				Parser<Term> apps = term.rep(false).map((nodes) -> {
					Term t = null;

					for (Term n : nodes) {
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
				Term t = expr.parse(buf);
				return t;
			}
		}
	}


	public class FirstOrder implements TermParserBuilder {

		private final Lexer l;

		public final Parser<String> openpar;
		public final Parser<String> closepar;
		public final Parser<String> comma;
		public final Parser<String> id;
		public final Parser<String> num;
		public final Parser<String> sym;

		/**
		 * Creates a new TermParserBuilder. Another lexer can be passed as an argument.
		 * @param l possibly pre-initialized lexer. Be careful, because certain tokens are needed. Check code of
		 *          constructor to see which ones ( '(', ')', '.', '\', ids, nums.
		 */
		public FirstOrder(Lexer l) {
			this.l = l;

			l.addIgnore(" ");
			openpar = l.tok("\\(");
			closepar = l.tok("\\)");
			comma = l.tok("\\,");
			id = l.match("[A-Za-z_][0-9A-Za-z_]*");
			num = l.match("[0-9]+");
			// FIXME Parser: id would not match now anymore...
			sym = l.match("[^(), a-zA-Z_0-9]+"); // everything else is a symbol.
		}

		public Parser<Term> parser(TermList list, Function<String, Boolean> isVar) {
			return new TermParser(list, isVar);
		}

		/**
		 * Parser for terms. It uses the lexer from the outside builder.
		 */
		public class TermParser extends Parser<Term> {

			final TermList list;
			public final Parser.PostInit<Term> expr;
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
				Parser<Term> integer = num.map(i -> Fun.create(list, i, Collections.emptyList()));

				// argument list
				Parser<List<Term>> args = openpar.thenRight(expr.then(comma.thenRight(expr).rep(true)).opt()).thenLeft(closepar).map(
						ts -> {
							LinkedList<Term> l = new LinkedList<>();
							if(ts.isDef) {
								l.add(ts.get().a);
								l.addAll(ts.get().b.asList());
							}

							return l;
						}
				);

				// variables like "x" but also "f(...)".
				Parser<Term> var = id.then(args.opt()).map(
						s -> s.b.isDef ? Fun.create(list, s.a, s.b.get()) : (isVar.apply(s.a) ? Var.create(list, s.a) : Fun.create(list, s.a, Collections.emptyList()))
				);

				// everything else, eg single symbols like +, but also +(a,b)
				Parser<Term> fun = sym.then(args.opt()).map(
						s -> s.b.isDef ? Fun.create(list, s.a, s.b.get()) : Fun.create(list, s.a, Collections.emptyList())
				);

				/**
				 * term = integer | real | var | '(' expr ')' | lambda
				 */
				Parser<Term> term = fun.or(var).or(integer);

				expr.set(term);
			}

			@Override
			public Term parse(Buffer buf) {
				// parse only builds up the tree.
				// it is later inserted into the list
				Term t = expr.parse(buf);
				return t;
			}

			public String toString() {
				return expr.get().toString();
			}
		}
	}
}
