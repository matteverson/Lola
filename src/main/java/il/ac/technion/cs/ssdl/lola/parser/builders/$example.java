package il.ac.technion.cs.ssdl.lola.parser.builders;

import java.io.*;
import java.util.*;

import il.ac.technion.cs.ssdl.lola.parser.*;
import il.ac.technion.cs.ssdl.lola.parser.builders.AST.*;
import il.ac.technion.cs.ssdl.lola.parser.lexer.*;

public class $example extends Elaborator {
  private static boolean equalsIgnoringTrivia(final String s1, final String s2) {
    return s1.replace(" ", "").replace("\n", "").replace("\t", "").replace("\r", "")
        .equals(s2.replace(" ", "").replace("\n", "").replace("\t", "").replace("\r", ""));
  }

  // String example = "";
  String expected;

  public $example(final Token token) {
    super(token);
    expectedElaborators = new ArrayList<>(Arrays.asList(new String[] { "$resultsIn" }));
    state = Automaton.List;
  }
  @Override public boolean accepts(final AST.Node b) {
    return state == Automaton.List && !(b instanceof SnippetToken)
        && (!(b instanceof Elaborator) && !(b instanceof $Find) || expectedElaborators.contains(b.name()));
  }
  @Override public void adopt(final AST.Node b) {
    if (Automaton.List == state)
      // example += b.token.text;
      if (!(b instanceof $resultsIn))
      list.add(b);
      else {
      elaborators.add((Elaborator) b);
      expected = (($resultsIn) b).getText();
      state = Automaton.Done;
      }
  }
  public void checkExample(final Parser p, final Lexi l) throws IOException {
    final String result = p.parseExample(l, list.stream().map(s -> s.text()).reduce("", (s1, s2) -> s1 + s2));
    if (!equalsIgnoringTrivia(result, expected))
      throw new AssertionError("expected: [" + expected + "] but was: [" + result + "]");
  }
};
