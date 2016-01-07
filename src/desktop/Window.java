import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JComponent;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

public class Window extends Val {
  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("gui#Window"))
      .put(new BuiltinFunc("gui#Window#__new__") {
        public Val calli(Val self, ArrayList<Val> args) {
          // TODO: Clean up this dirty 'array as pointer' hack.
          final JFrame[] frameptr = new JFrame[1];
          final Panel[] panelptr = new Panel[1];
          invokeAndWait(new Runnable() {
            public void run() {
              JFrame frame = frameptr[0] = new JFrame("MyJFrame");
              Panel panel = panelptr[0] = new Panel();
              frame.add(panel);
              frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              frame.pack();
              frame.setVisible(true);
            }
          });
          return new Window(frameptr[0], panelptr[0]);
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
                window.panel.setPreferredSize(new Dimension(width, height));
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
            // explicitly indicate that getBounds is threadsafe.
            // So I'm playing it safe here. Look into this more.
            invokeAndWait(new Runnable() {
              public void run() {
                dims[0] = window.panel.getPreferredSize();
              }
            });

            return List.from(
                Num.from(dims[0].width), Num.from(dims[0].height));
          } else {
            throw new Err(
                "Expected 0 or 2 arguments but found " + args.size());
          }
        }
      })
      .put(new BuiltinFunc("gui#Window#drawText") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 4);
          Window w = self.as(Window.class, "self");
          String text = args.get(0).as(Str.class, "arg 0").val;
          int x = args.get(1).as(Num.class, "arg 1 (x)").val.intValue();
          int y = args.get(2).as(Num.class, "arg 2 (y)").val.intValue();
          int size = args.get(3).as(Num.class, "arg 3 (size)").val.intValue();
          w.panel.drawText(text, x, y, size);
          return self;
        }
      })
      .hm;

  // WARNING: The 'frame' member should never be touched outside the event
  // dispatch thread.
  // As you can see in gui#Window#__new__ function above, when a Window
  // object is created, we queue up a Runnable that sets this Window's JFrame.

  private final JFrame frame;
  private final Panel panel;
  private Window(JFrame frame, Panel panel) {
    this.frame = frame;
    this.panel = panel;
  }
  public HashMap<String, Val> getMeta() { return MM; }

  // At first I thought about having separate a separate 'Panel' class,
  // but why enforce abstractions here? I figure I could do that in the
  // language itself.
  // If performance really becomes an issue, I'll worry about it then.
  private static final class Panel extends JComponent implements ComponentListener {
    public static final long serialVersionUID = 42L;

    // TODO: Worry about consequences of concurrency.
    // TODO: Worry about screens bigger than 4k x 4k.
    private final BufferedImage image =
        new BufferedImage(4000, 4000, BufferedImage.TYPE_INT_ARGB);

    public Panel() {
      super();
      addComponentListener(this);
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.drawImage(image, 0, 0, null);
    }

    public void componentHidden(ComponentEvent e) {
    }
    public void componentMoved(ComponentEvent e) {
    }
    public void componentResized(ComponentEvent e) {
    }
    public void componentShown(ComponentEvent e) {
    }

    public void drawText(String text, int x, int y, int fontsize) {
      Graphics2D g = image.createGraphics();
      g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontsize));
      g.setColor(Color.black);
      g.drawString(text, x, y);
      // g.fillOval(0, 0, 50, 50);
      g.dispose();
      repaint();
    }
  }

  private static BufferedImage copy(BufferedImage image) {
    BufferedImage copy = new BufferedImage(
        image.getWidth(), image.getHeight(), image.getType());
    Graphics2D g = copy.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return copy;
  }

  private static void invokeAndWait(Runnable r) {
    try { SwingUtilities.invokeAndWait(r); }
    catch (InterruptedException e) { throw new Err(e); }
    catch (InvocationTargetException e) { throw new Err(e); }
  }
}
