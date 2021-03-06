package il.ac.technion.cs.ssdl.lola.parser.builders;

import il.ac.technion.cs.ssdl.lola.parser.lexer.*;
import il.ac.technion.cs.ssdl.lola.parser.re.*;
import il.ac.technion.cs.ssdl.lola.parser.re.RegExp.*;

public class TriviaToken extends AST.Node implements RegExpable {
  public TriviaToken(final Token token) {
    super(token);
  }
  public String getText() {
    return token.text;
  }
  @Override public RegExp toRegExp() {
    return new Atomic.Host(token.text);
  }
}