import java.util.ArrayList;
import javax.swing.*;

public class Gui {
  public static final Blob MODULE = new Blob(
      Val.MMModule,
      new Val.Hmb()
          .put(new BuiltinFunc("test") {
            public Val calli(Val self, ArrayList<Val> args) {
              javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  JFrame frame = new JFrame("HelloWorldSwing");
                  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                  JLabel label = new JLabel("Hello World");
                  frame.getContentPane().add(label);

                  frame.pack();
                  frame.setVisible(true);
                }
              });
              return Nil.val;
            }
          })
          .put(Window.MM)
          .hm
      );
}
