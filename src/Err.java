import java.util.ArrayList;

public final class Err extends RuntimeException {
  public static final long serialVersionUID = 42L;

  private final ArrayList<Traceable> trace = new ArrayList<Traceable>();
  public Err(Exception exc) {
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
}
