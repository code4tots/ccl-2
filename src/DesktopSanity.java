import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class DesktopSanity extends Sanity {

  static {
    BUILTIN_SCOPE
        .put(new FunctionValue("print") {
          public Value calli(Context c, ArrayList<Value> args) {
            expectArgLen(c, args, 1);
            StringValue sv = asStringValue(c, args.get(0), "argument 0");
            System.out.println(sv.value);
            return sv;
          }
        })
        .put(new FunctionValue("read") {
          public final Value calli(Context c, ArrayList<Value> args) {
            expectArgLen(c, args, 1);
            return toStringValue(readFile(
                asStringValue(c, args.get(0), "argument 0").value));
          }
        });
  }

  public static void main(String[] args) {
    Sanity.main(args);
  }

}
