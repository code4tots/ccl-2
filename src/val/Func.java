import java.util.ArrayList;

public abstract class Func extends Val implements Traceable {
  public abstract Val call(Val self, ArrayList<Val> args);
}
