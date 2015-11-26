import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.*;

public class EasyDesktop extends Easy {

  static public WindowValue window = null;

  static public ClassValue classWindow =
      new ClassValue("Window", classBuiltinObject);

  static public class WindowValue extends Value {
    public final JFrame frame;
    public WindowValue() {
      frame = new JFrame("Hello world");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
    }
    public ClassValue getType() { return classWindow; }
    public boolean equals(Value value) {
      return this == value;
    }
    public boolean isTruthy() {
      return true;
    }
  }

  static {
    BUILTIN_SCOPE
        .put(classWindow);

    classWindow
      .put(new BuiltinFunctionValue("__new__") {
        public Value call(ArrayList<Value> args) {
          if (window == null) {
            window = new WindowValue();
          }
          return window;
        }
      });
  }

}
