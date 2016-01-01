import java.util.ArrayList;
import java.util.HashMap;

public abstract class BuiltinFunc extends Func {

  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final String getTraceMessage() {
    return "\nin builtin function " + name;
  }
  public final Val call(Val self, ArrayList<Val> args) {
    try { return calli(self, args); }
    catch (final Err e) { e.add(this); throw e; }
    catch (final Exception e) { throw new Err(e); }
  }
  public abstract Val calli(Val self, ArrayList<Val> args);
}
