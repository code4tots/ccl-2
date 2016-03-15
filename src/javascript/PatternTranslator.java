package com.ccl.javascript;

import com.ccl.core.*;

// TODO: Make this node comptabile so that it doesn't
// rely on member attribute destructuring assignment.
public final class PatternTranslator
    extends PatternVisitor<StringBuilder> {

  public void visitList(Pattern.List pattern, StringBuilder sb) {
    sb.append("[");
    for (Pattern p: pattern.args) {
      visit(p, sb);
      sb.append(",");
    }
    for (Pattern p: pattern.optargs) {
      visit(p, sb);
      sb.append(",");
    }
    if (pattern.vararg != null) {
      sb.append("...scope.cclid_");
      sb.append(pattern.vararg);
    }
    sb.append("]");
  }

  public void visitName(Pattern.Name pattern, StringBuilder sb) {
    sb.append("scope.cclid_");
    sb.append(pattern.name);
  }
}
