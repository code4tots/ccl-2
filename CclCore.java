/*

types
  num (Double)
  str (String)
  list (ArrayList)
  dict (HashMap)
  lambda (Lambda)
  builtin (represented as Java object none of the above)

builtins define their own behavior when retrieving or assigning
attributes, or when trying to call it like a function.

*/
import java.util.ArrayList;
import java.util.HashMap;

public class CclCore {

  public static class ControlFlowException extends Error {
    public Object value;
  }

  public static class ReturnException extends ControlFlowException {
    public ReturnException(Object value) { this.value = value; }
  }

  public static class BreakException extends ControlFlowException {
    public BreakException(Object value) { this.value = value; }
  }

  public static HashMap findContainingContext(HashMap context, String name) {
    if (context.containsKey(name))
      return context;
    if (context.containsKey("__parent__"))
      return findContainingContext((HashMap) context.get("__parent__"), name);
    throw new Error("Name " + name + " not found");
  }

  public static void assign(HashMap context, HashMap target, Object value) {
    if (target.get("type").equals("name")) {
      findContainingContext(context, (String) target.get("name")).put(target.get("name"), value);
      return;
    } else if (target.get("type").equals("list")) {
      ArrayList target_items = (ArrayList) target.get("items");
      ArrayList value_items = (ArrayList) value;
      if (target_items.size() != value_items.size())
        throw new Error("Invalid assignment length");
      for (int i = 0; i < target_items.size(); i++) {
        assign(context, (HashMap) target_items.get(i), value_items.get(i));
      }
      return;
    } else {
      throw new Error("Assigning " + ((String) target.get("type")) + " not supported");
    }
  }
}
