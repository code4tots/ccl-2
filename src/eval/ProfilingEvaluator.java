import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;

public class ProfilingEvaluator extends Evaluator {

  private static final HashMap<String, Long> totals =
      new HashMap<String, Long>();

  private static final HashMap<String, Long> reps =
      new HashMap<String, Long>();

  public static synchronized void record(String name, long time) {
    Long total = totals.get(name);
    if (total == null)
      totals.put(name, Long.valueOf(time));
    else
      totals.put(name, total + time);

    Long rep = reps.get(name);
    if (rep == null)
      reps.put(name, Long.valueOf(1));
    else
      reps.put(name, rep + 1);
  }

  private static ArrayList<java.util.Map.Entry<String, Long>>
      getSortedResults() {
    ArrayList<java.util.Map.Entry<String, Long>> results =
        new ArrayList<java.util.Map.Entry<String, Long>>();
    Iterator<java.util.Map.Entry<String, Long>> it = totals.entrySet().iterator();
    while (it.hasNext())
      results.add(it.next());
    Collections.sort(
        results, new Comparator<java.util.Map.Entry<String, Long>>() {
      public boolean equals(Object other) {
        return this == other;
      }
      public int compare(
          java.util.Map.Entry<String, Long> a,
          java.util.Map.Entry<String, Long> b) {
        return
            Double.valueOf(a.getValue() / reps.get(a.getKey())
                .doubleValue()).compareTo(
                    b.getValue() / reps.get(b.getKey()).doubleValue());
      }
    });
    return results;
  }

  public static synchronized String getResultSummary() {
    StringBuilder sb = new StringBuilder();
    Iterator<java.util.Map.Entry<String, Long>> it = getSortedResults().iterator();
    while (it.hasNext()) {
      java.util.Map.Entry<String, Long> entry = it.next();
      sb.append(entry.getKey());
      for (int i = 0; i < 25 - entry.getKey().length(); i++)
        sb.append(" ");
      sb.append(" -> ");
      sb.append("average (nanosec) = ");
      String dat = Long.valueOf(entry.getValue() / reps.get(entry.getKey())).toString();
      sb.append(dat);
      for (int i = 0; i < 15 - dat.length(); i++)
        sb.append(" ");
      sb.append("reptitions = ");
      dat = reps.get(entry.getKey()).toString();
      sb.append(dat);
      for (int i = 0; i < 8 - dat.length(); i++)
        sb.append(" ");
      sb.append(" total (millisec) = ");
      sb.append(entry.getValue() / 1000000.0);
      sb.append("\n");
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
