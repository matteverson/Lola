package il.ac.technion.cs.ssdl.lola.parser.builders;

import il.ac.technion.cs.ssdl.lola.parser.builders.AST.*;
import il.ac.technion.cs.ssdl.lola.parser.lexer.*;
import il.ac.technion.cs.ssdl.lola.parser.re.*;

public class $closer extends Elaborator implements RegExpable {
  public String closer;

  public $closer(final Token token) {
    super(token);
  }
  @Override public boolean accepts(final Node b) {
    return closer == null;
  }
  @Override public void adopt(final AST.Node b) {
    list.add(b);
    if (b.token.isTrivia())
      return;
    closer = b.token.text;
    state = Automaton.Done;
  }
  @Override public RegExp toRegExp() {
    return new RegExp.Atomic.Host(closer);
  }
}
