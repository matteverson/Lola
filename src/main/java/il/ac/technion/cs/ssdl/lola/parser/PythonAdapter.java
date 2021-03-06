package il.ac.technion.cs.ssdl.lola.parser;

import java.util.*;

import org.python.util.*;

import il.ac.technion.cs.ssdl.lola.utils.*;

public class PythonAdapter {
  private static String generateVriable() {
    return "val" + (int) (Math.random() * Integer.MAX_VALUE);
  }

  private final PythonInterpreter pi = new PythonInterpreter();
  private final Stack<String> scope = new Stack<>();
  Stack<String> forEachScopes = new Stack<>();
  Stack<String> forEachIterationScopes = new Stack<>();

  public PythonAdapter() {
    exec("class Scope(object):\n\tpass");
    exec("class Identifier(object):\n\tpass");
  }
  public void addCharVariable(final String name, final String content) {
    addVariable(name, "\"" + content + "\"");
  }
  public void addIdentifier(final String name, final String matching) {
    exec(scope() + name + "= Identifier()");
    if (scope.empty()) {
      exec(name + ".name = " + matching);
      exec("if '" + name + "s' not in locals():\n\t" + name + "s = list()");
      exec(name + "s.append(" + name + ")");
    } else {
      exec(scope() + name + ".name = " + matching);
      exec("if not hasattr(" + scopeName() + ", '" + name + "s'):\n\t" + scope() + name + "s = list()");
      exec(scope() + name + "s.append(" + scope() + name + ")");
    }
    // + "\nif isinstance(" + val + ",basestring):\n\t" + val + " = '\"' + "
    // + val + " + '\"'\n"
  }
  public void addStringVariable(final String name, final String content) {
    addVariable(name, "\'" + content + "\'");
  }
  public void addVariable(final String name, final String content) {
    if (scope.empty()) {
      exec(name + " = " + content);
      exec("if '" + name + "s' not in locals():\n\t" + name + "s = list()");
      exec(name + "s.append(" + name + ")");
    } else {
      exec(scope() + name + " = " + content);
      exec("if not hasattr(" + scopeName() + ", '" + name + "s'):\n\t" + scope() + name + "s = list()");
      exec(scope() + name + "s.append(" + scope() + name + ")");
    }
  }
  public String eavluateStringExpression(final String e) {
    final String val = generateVriable();
    exec(val + "= " + e + "\nif isinstance(" + val + ",Identifier) or isinstance(" + val + ",Scope):\n\t" + val + " = " + val + ".name\n" + val
        + " = " + val + ".__str__()\n");
    return pi.get(val).asString();
  }
  public void enterScope(final String name, final String matching) {
    exec(scope() + name + "= Scope()");
    final String escapedMatching = matching.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    exec(scope() + name + ".name = '" + escapedMatching + "'");
    if (scope.empty()) {
      exec("if '" + name + "s' not in locals():\n\t" + name + "s = list()");
      exec(name + "s.append(" + name + ")");
    } else {
      exec("if not hasattr(" + scopeName() + ", '" + name + "s'):\n\t" + scope() + name + "s = list()");
      exec(scope() + name + "s.append(" + scope() + name + ")");
    }
    scope.push(name);
  }
  public boolean evaluateBooleanExpression(final String e) {
    exec("if " + e + ":\n\tval=1\nelse:\n\tval=0");
    return pi.get("val").asInt() == 1;
  }
  public void exitScope() {
    scope.pop();
  }
  public void forEachAfter() {
    forEachScopes.pop();
    if (!forEachIterationScopes.isEmpty())
      forEachIterationScopes.pop();
    if (!forEachIterationScopes.isEmpty())
      exec(forEachIterationScopes.peek());
  }
  public int forEachBeforeAndGetIterationsNum(final String e) {
    final String var = generateVriable();
    forEachScopes.push(var);
    // exec("print locals()");
    exec(var + " = " + e);
    return pi.eval("len(" + var + ")").asInt();
  }
  public void forEachBeforeIteration(final int i) {
    final String assignment = "_=" + forEachScopes.peek() + "[" + i + "]";
    if (i != 0)
      forEachIterationScopes.pop();
    forEachIterationScopes.push(assignment);
    exec(assignment);
  }
  public void runShellCode(final String code) {
    exec(code);
  }
  public String scope() {
    String $ = "";
    for (final String s : scope)
      $ += s + ".";
    return $;
  }
  private void exec(final String s) {
    Printer.printCommand(s);
    pi.exec(s);
  }
  private String scopeName() {
    final String tmp = scope();
    return tmp.substring(0, tmp.length() - 1);
  }
}
