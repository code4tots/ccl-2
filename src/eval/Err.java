import java.util.ArrayList;

public final class Err extends RuntimeException {
  public static final long serialVersionUID = 42L;

  private final ArrayList<Traceable> trace = new ArrayList<Traceable>();
  public Err(Throwable exc) {
    super(exc);
  }
  public Err(String message) {
    super(message);
  }
  public void add(Traceable tr) {
    trace.add(tr);
  }
  public String getTraceString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < trace.size(); i++)
      sb.append(trace.get(i).getTraceMessage());
    return sb.toString();
  }

  public static <T> T notNull(T t) {
    if (t == null)
      throw new Err("Unexpected null pointer");
    return t;
  }

  public static void expectArgRange(ArrayList<Val> args, int min, int max) {
    if (args.size() < min || args.size() > max)
      throw new Err(
          "Expected " + min + " to " + max + " arguments but found " +
          args.size() + " arguments.");
  }

  public static void expectArglen(ArrayList<Val> args, int len) {
    if (args.size() != len)
      throw new Err(
          "Expected " + len + " arguments but found " +
          args.size() + " arguments.");
  }

  public static void expectMinArglen(ArrayList<Val> args, int len) {
    if (args.size() < len)
      throw new Err(
          "Expected at least " + len + " arguments but found only " +
          args.size() + " arguments.");
  }

  public static void expectArglens(ArrayList<Val> args, int... lens) {
    for (int i = 0; i < lens.length; i++)
      if (args.size() == lens[i])
        return;
    StringBuilder sb = new StringBuilder("Expected ");
    for (int i = 0; i < lens.length-1; i++)
      sb.append(lens[i] + " or ");
    sb.append(lens[lens.length-1] + " arguments but found " + args.size());
    throw new Err(sb.toString());
  }
}
