import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public class ProfilingEvaluator extends Evaluator {

  private static final HashMap<String, Long> times =
      new HashMap<String, Long>();

  public static synchronized void record(String name, long time) {
    Long total = times.get(name);
    if (total == null)
      times.put(name, total = Long.valueOf(0));
    else
      times.put(name, total + time);
  }

  public static synchronized String getResultSummary() {
    StringBuilder sb = new StringBuilder();
    Iterator<HashMap.Entry<String, Long>> it = times.entrySet().iterator();
    while (it.hasNext()) {
      HashMap.Entry<String, Long> entry = it.next();
      sb.append(entry.getKey());
      for (int i = 0; i < 25 - entry.getKey().length(); i++)
        sb.append(" ");
      sb.append(" -> ");
      sb.append(entry.getValue() / 1000000.0);
      sb.append(" milliseconds\n");
    }
    return sb.toString();
  }

  public ProfilingEvaluator(Scope scope) {
    super(scope);
  }

  public Val visit(Ast node) {
    long start = System.nanoTime();
    Val result = super.visit(node);
    long end = System.nanoTime();
    record(node.getClass().getName(), end - start);
    return result;
  }
}
