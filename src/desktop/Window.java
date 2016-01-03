import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import java.awt.*;

public class Window extends Val {
  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("gui#Window"))
      .put(new BuiltinFunc("gui#Window#__new__") {
        public Val calli(Val self, ArrayList<Val> args) {
          final Window window = new Window();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              window.frame = new JFrame("MyJFrame");
              window.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              window.frame.pack();
              window.frame.setVisible(true);
            }
          });
          return window;
        }
      })
      .put(new BuiltinFunc("gui#Window#dim") {
        public Val calli(Val self, ArrayList<Val> args) {
          final Window window = self.as(Window.class, "self");
          if (args.size() == 2) {
            final int width =
                args.get(0).as(Num.class, "argument 0 (width)")
                .val.intValue();
            final int height =
                args.get(1).as(Num.class, "argument 1 (height)")
                .val.intValue();
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                window.frame.setPreferredSize(new Dimension(width, height));
                window.frame.pack();
              }
            });
            return self;
          } else if (args.size() == 0) {
            // 'dims' basically acts as a pointer here.
            // TODO: This seems like icky style. Think of something more
            // elegant.
            final Dimension[] dims = new Dimension[1];

            // TODO: Hmm. invokeAndWait seems pretty pricey for just reading
            // the size of a frame. The documentation doesn't seem to
            // explicitly indicate that getPreferredSize is threadsafe.
            // So I'm playing it safe here. Look into this more.
            try {
              SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                  dims[0] = window.frame.getPreferredSize();
                }
              });
            } catch (InterruptedException e) {
              throw new Err(e);
            } catch (java.lang.reflect.InvocationTargetException e) {
              throw new Err(e);
            }

            return List.from(
                Num.from(dims[0].width), Num.from(dims[0].height));
          } else {
            throw new Err(
                "Expected 0 or 2 arguments but found " + args.size());
          }
        }
      })
      .hm;

  // WARNING: The 'frame' member should never be touched outside the event
  // dispatch thread.
  // As you can see in gui#Window#__new__ function above, when a Window
  // object is created, we queue up a Runnable that sets this Window's JFrame.

  // TODO: This of course depends on the assumption that the EDT runs
  // Runnables in the order they are queued up. Check the documentation
  // to verify that this is safe to assume.

  private JFrame frame = null;
  private Window() {}
  public HashMap<String, Val> getMeta() { return MM; }
}
