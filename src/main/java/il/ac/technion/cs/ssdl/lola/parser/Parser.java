package il.ac.technion.cs.ssdl.lola.parser;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import il.ac.technion.cs.ssdl.lola.parser.builders.*;
import il.ac.technion.cs.ssdl.lola.parser.lexer.*;
import il.ac.technion.cs.ssdl.lola.parser.re.*;
import il.ac.technion.cs.ssdl.lola.parser.tokenizer.*;
import il.ac.technion.cs.ssdl.lola.utils.*;

public class Parser {
	private static final String BUILDERS_PATH = "il.ac.technion.cs.ssdl.lola.parser.builders.$";
	private static String lolaEscapingCharacter;
	static Map<String, $Find> userDefinedKeywords = new HashMap<>();

	private static Keyword newKeyword(final Token t) {
		// some reflection, to create a keyword by its name...
		final String name = t.text.replace(lolaEscapingCharacter, "");
		try {
			return (Keyword) Class.forName(BUILDERS_PATH + name).getConstructor(Token.class).newInstance(t);
		} catch (ClassNotFoundException | NoClassDefFoundError e) {
			try {
				return (Keyword) Class.forName(BUILDERS_PATH + toUpperCaseClass(name)).getConstructor(Token.class)
						.newInstance(t);
			} catch (final Exception e1) {
				if (userDefinedKeywords.containsKey(name))
					return new $UserDefinedKeyword(t, userDefinedKeywords.get(name).list());
				e1.printStackTrace();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String toUpperCaseClass(final String name) {
		return name.substring(0, 1) + name.substring(1, 2).toUpperCase() + name.substring(2);
	}

	private final Tokenizer tokenizer;
	private final Stack<Builder> stack = new Stack<>();

	public Parser(final Reader stream) {
		tokenizer = new Tokenizer(stream);
		lolaEscapingCharacter = Tokenizer.lolaEscapingCharacter;
	}

	/* meanwhile this function is for testing */
	public List<Lexi> directives() throws IOException {
		final List<Lexi> $ = new ArrayList<>();
		Token t;
		while ((t = tokenizer.next_token()) != null)
			if (t.isKeyword())
				$.add(directive(newKeyword(t)));
		return $;
	}

	public List<String> parse() throws IOException {
		final Chain<Bunny, Lexi> chain = generateChain();
		applyNonImmediateDirectives(chain);
		return chain2stringList(chain);
	}

	public String parseExample(final Lexi l, final String example) throws IOException {
		final Chain<Bunny, Lexi> chain = new Chain<>(string2bunnies(example));
		chain.addFirst(l);
		applyNonImmediateDirectives(chain);
		return chain2string(chain);
	}

	public List<Bunny> string2bunnies(final String s) throws IOException {
		return tokenizerToBunnies(new Tokenizer(new StringReader(s)));
	}

	private boolean anyAccepts(final Keyword kw) {
		for (final Builder b : stack)
			if (b.accepts(kw))
				return true;
		return false;
	}

	/* Execute rule and add new tokens to tokenizer. */
	private void applyImmediateDirective(final GeneratingKeyword kw) throws IOException {
		Printer.printTokens(tokenizer);
		tokenizer.addTokens(new StringReader(kw.generate(new PythonAdapter())));
		Printer.printTokens(tokenizer);
	}

	/** meanwhile, this seems to be just the #Find keyword */
	private void applyLexi(final Chain<Bunny, Lexi> chain, final Matcher ruller) {
		final $Find kw = ($Find) ruller.lexi.keyword;
		final PythonAdapter a = new PythonAdapter();
		Printer.printRe(ruller.re);
		ruller.re.apply(a);
		for (final Elaborator e : kw.elaborators)
			if (e instanceof ExecutableElaborator)
				((ExecutableElaborator) e).execute(ruller.interval(), a, this);
		ruller.interval().earmark();
	}

	/**
	 * This is the FixedPoint algorithm. Kind of. Gets a chain of bunnies and
	 * apply lexies that are in it until no lexi can be applied.
	 */
	@SuppressWarnings("unchecked")
	private void applyNonImmediateDirectives(final Chain<Bunny, Lexi> chain) {
		boolean again = true;
		while (again) {
			again = false;
			final List<Lexi> directives = new ArrayList<>();
			final List<Matcher> matchers = new ArrayList<>();
			final List<Anchor> anchors = new ArrayList<>();
			final List<Anchor> satiatedAnchors = new ArrayList<>();
			chain.printChain();
			for (final Chain<Bunny, Lexi>.Node node : chain) {
				if (node.get() instanceof Lexi) {
					directives.add((Lexi) node.get());
					continue;
				}
				if (!(node.get() instanceof TriviaBunny))
					startNewMatchers(chain, directives, matchers, anchors, node);
				final List<Matcher> satiatedMatchers = feedMatchersGetSatiated(matchers, node);
				satiatedAnchors.addAll((List<Anchor>) (List<?>) feedMatchersGetSatiated(anchors, node));
				for (final Anchor a : satiatedAnchors)
					if (matchers.stream().filter(m -> m.lexi == a.lexi && m.startsBeforeOrTogether(a)).count() == 0)
						a.explode();
				if (!satiatedMatchers.isEmpty()) {
					applyLexi(chain, getRuller(satiatedMatchers));
					again = true;
					break;
				}
			}
			if (!again && !satiatedAnchors.isEmpty())
				satiatedAnchors.get(0).explode();
		}
	}

	private void startNewMatchers(final Chain<Bunny, Lexi> chain, final List<Lexi> directives, final List<Matcher> ms,
			final List<Anchor> as, final Chain<Bunny, Lexi>.Node n) {
		ms.addAll(directives.stream().map(le -> new Matcher(le, chain, n.before())).collect(Collectors.toList()));
		as.addAll(directives.stream().filter(le -> le.hasAnchor()).map(le -> new Anchor(le, chain, n.before()))
				.collect(Collectors.toList()));
	}

	private String chain2string(final Chain<Bunny, Lexi> chain) {
		final List<String> li = chain2stringList(chain);
		String $ = "";
		for (final String s : li)
			$ += s;
		return $;
	}

	private List<String> chain2stringList(final Chain<Bunny, Lexi> chain) {
		final List<String> $ = new ArrayList<>();
		for (final Chain<Bunny, Lexi>.Node node : chain)
			if (!(node.get() instanceof Lexi))
				$.add(node.get().text());
		return $;
	}

	/*
	 * Generates a lexi.
	 *
	 * @param c - the keyword that initiated the lexi
	 */
	private Lexi directive(final Keyword c) throws IOException {
		stack.push(c);
		Token t;
		while ((t = tokenizer.next_token()) != null)
			if (isLeaf(t)) {
				if (!percolate(t.isSnippet() && stack.peek().accepts(new SnippetToken(t)) ? new SnippetToken(t)
						: !t.isTrivia() ? new HostToken(t) : new TriviaToken(t)) && stack.isEmpty()) {
					tokenizer.unget();
					Printer.printAST(c);
					return new Lexi(c);
				}
			} else { // t is keyword
				final Keyword kw = newKeyword(t);
				if (!anyAccepts(kw)) {
					tokenizer.unget();
					break;
				}
				stack.push(kw);
			}
		while (!stack.isEmpty()) {
			final Builder b = stack.pop();
			b.done();
			percolate(b);
		}
		Printer.printAST(c);
		return new Lexi(c);
	}

	private List<Matcher> feedMatchersGetSatiated(final List<? extends Matcher> ms, final Chain<Bunny, Lexi>.Node n) {
		final List<Matcher> $ = new ArrayList<>();
		final List<Matcher> toRemove = new ArrayList<>();
		for (final Matcher m : ms)
			if (!m.eats(n.get()))
				toRemove.add(m);
			else {
				m.feed(n.get());
				if (m.satiated() && !m.interval().earmarked())
					$.add(m);
			}
		for (final Matcher m : toRemove)
			ms.remove(m);
		return $;
	}

	/**
	 * Generates a chain of bunnies (Host tokens + Lola directives) We start
	 * with the tokenizer and whenever we come up an immedaite directive we
	 * execute it, resulting in a new stream of tokens to parse, which we add to
	 * the tokenizer at the place where the lexi was found.
	 */
	private Chain<Bunny, Lexi> generateChain() throws IOException {
		return new Chain<>(tokenizerToBunnies(tokenizer));
	}

	private Matcher getRuller(final List<Matcher> candidates) {
		return candidates.stream().reduce((x, y) -> y.interval().strictlyContainedIn(x.interval()) ? y : x).get();
	}

	private boolean isLeaf(final Token b) {
		return b.isSnippet() || b.isTrivia() || b.isHost();
	}

	private boolean percolate(AST.Node c) {
		while (!stack.isEmpty()) {
			final Builder b = stack.peek();
			if (c.column() < b.column() || !b.accepts(c)) {
				b.done();
				stack.pop();
				percolate(b);
				continue;
			}
			b.adopt(c);
			if (!b.mature())
				return true;
			stack.pop();
			c = b;
			if (stack.isEmpty())
				return true;
		}
		return false;
	}

	private List<Bunny> tokenizerToBunnies(final Tokenizer tokenizer) throws IOException {
		final List<Bunny> $ = new ArrayList<>();
		while (true) {
			final Token t = tokenizer.next_token();
			if (t == null)
				break;
			if (!t.isKeyword())
				$.add(t.isHost() ? new HostBunny(t) : new TriviaBunny(t));
			else {
				final Lexi lexi = directive(newKeyword(t));
				if (lexi.isImmediate())
					applyImmediateDirective((GeneratingKeyword) lexi.keyword);
				else {
					for (final Elaborator e : lexi.keyword.elaborators)
						if (e instanceof $example)
							(($example) e).checkExample(this, lexi);
					$.add(lexi);
					if (lexi.keyword.snippet() != null)
						userDefinedKeywords.put(lexi.keyword.snippet().getExpression(), ($Find) lexi.keyword);
				}
			}
		}
		return $;
	}
}