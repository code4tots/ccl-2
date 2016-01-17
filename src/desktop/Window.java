import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class Window extends Val {
  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("gui#Window"))
      .put(new BuiltinFunc("gui#Window#__new__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return getInstance();
        }
      })
      .put(new BuiltinFunc("gui#Window#dim") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglens(args, 0, 2);
          Window win = self.as(Window.class, "self");
          if (args.size() == 0) {
            return List.from(
                Num.from(win.width),
                Num.from(win.height));
          } else {
            final int width = args.get(0).as(
                Num.class, "arg 0 (width)").asIndex();
            final int height = args.get(1).as(
                Num.class, "arg 1 (height)").asIndex();
            win.resize(width, height);
            return self;
          }
        }
      })
      .put(new BuiltinFunc("gui#Window#fontsize") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglens(args, 0, 1);
          Window win = self.as(Window.class, "self");
          if (args.size() == 0) {
            return Num.from(win.fontsize);
          } else {
            win.fontsize = args.get(0).as(Num.class, "fontsize").asIndex();
            return self;
          }
        }
      })
      .put(new BuiltinFunc("gui#Window#color") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglens(args, 0, 1);
          Window win = self.as(Window.class, "self");
          if (args.size() == 0) {
            return Num.from(win.color.getRGB() & 0xFFFFFF);
          } else {
            win.color =
                new Color(args.get(0).as(Num.class, "color").asIndex());
            return self;
          }
        }
      })
      .put(new BuiltinFunc("gui#Window#text") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 3);
          Window win = self.as(Window.class, "self");
          String text = args.get(0).as(Str.class, "arg 0").val;
          final int x = args.get(1).as(
              Num.class, "arg 1 (x)").asIndex();
          final int y = args.get(2).as(
              Num.class, "arg 2 (y)").asIndex();

          Graphics2D g = win.image.createGraphics();
          g.setColor(win.color);
          g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, win.fontsize));
          g.drawString(text, x, y);
          g.dispose();

          if (win.autoflush)
            win.flush();

          return self;
        }
      })
      .put(new BuiltinFunc("gui#Window#clear") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          self.as(Window.class, "self").clear();
          return Nil.val;
        }
      })
      // Take the next availble event.
      .put(new BuiltinFunc("gui#Window#event") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self.as(Window.class, "self").eventChannel.take();
        }
      })
      .hm;

  // 'Window' is a singleton. I would have multiple, except
  // when any one of them closes, everyone is going to close.
  // TODO: Consider making them available anyway.
  private static Window instance = null;

  private final Channel eventChannel;
  private final JFrame frame;
  private final JPanel panel;
  private volatile BufferedImage image =
      new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
  private Color color = Color.BLACK;
  private int fontsize = 12;
  private boolean autoflush = true;

  private volatile int width = 1, height = 1;

  public static Window getInstance() {
    if (instance == null)
      instance = new Window();
    return instance;
  }

  private Window() {
    if (instance != null)
      throw new Err("Tried to create multiple instances of Window");

    this.eventChannel = new Channel();

    final JFrame[] frameptr = new JFrame[1];
    final JPanel[] panelptr = new JPanel[1];
    invokeAndWait(new Runnable() {
      public void run() {
        JFrame frame = frameptr[0] = new JFrame();
        JPanel panel = panelptr[0] = new JPanel() {
          public static final long serialVersionUID = 42L;
          public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
          }
        };
        frame.add(panel);
        frame.addWindowListener(new WindowListener() {
          public void windowActivated(WindowEvent e) {}
          public void windowDeactivated(WindowEvent e) {}
          public void windowDeiconified(WindowEvent e) {}
          public void windowIconified(WindowEvent e) {}
          public void windowOpened(WindowEvent e) {}
          public void windowClosing(WindowEvent e) {
            // We could poison the commandChannel here, but
            // it's unnecessary since our defualt close operation
            // is EXiT_ON_CLOSE.
            // Furthermore, poison here, the end of the processing
            // thread will call close again, making this code be
            // run twice.
            // commandChannel.put(Nil.val);
          }
          public void windowClosed(WindowEvent e) {}
        });
        panel.addMouseListener(new MouseListener() {
          public void mouseClicked(MouseEvent e) {}
          public void mouseEntered(MouseEvent e) {}
          public void mouseExited(MouseEvent e) {}
          public void mousePressed(MouseEvent e) {}
          public void mouseReleased(MouseEvent e) {
            eventChannel.put(List.from(
                Str.from("click"),
                Num.from(e.getX()),
                Num.from(e.getY())));
          }
        });
        panel.addComponentListener(new ComponentListener() {
          public void componentHidden(ComponentEvent e) {}
          public void componentMoved(ComponentEvent e) {}
          public void componentResized(ComponentEvent e) {
            width = panel.getWidth();
            height = panel.getHeight();
          }
          public void componentShown(ComponentEvent e) {}
        });
        panel.setBackground(Color.WHITE);
        // I think only using DISPOSE_ON_CLOSE leaves around
        // some other swing threads for a second or two keeping
        // this alive.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
      }
    });
    this.frame = frameptr[0];
    this.panel = panelptr[0];
  }

  public HashMap<String, Val> getMeta() { return MM; }

  public void resize(final int width, final int height) {
    if (width > image.getWidth() || height > image.getHeight()) {
      BufferedImage newImage = new BufferedImage(
          2 * width + 1,
          2 * height + 1,
          BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = newImage.createGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      // This assignment is atomic.
      image = newImage;
    }
    invokeAndWait(new Runnable() {
      public void run() {
        panel.setPreferredSize(new Dimension(width, height));
        frame.pack();
        Window.this.width = panel.getWidth();
        Window.this.height = panel.getHeight();
      }
    });
  }

  public void flush() {
    panel.repaint();
  }

  public void clear() {
    Graphics2D g = image.createGraphics();
    g.setBackground(Color.WHITE);
    g.clearRect(0, 0, image.getWidth(), image.getHeight());
    g.dispose();
    if (autoflush)
      flush();
  }

  private static void invokeAndWait(Runnable r) {
    try { SwingUtilities.invokeAndWait(r); }
    catch (InterruptedException e) { throw new Err(e); }
    catch (InvocationTargetException e) { throw new Err(e); }
  }

  public static BufferedImage copy(BufferedImage image) {
    BufferedImage newImage = new BufferedImage(
        image.getWidth(), image.getHeight(), image.getType());
    Graphics2D g = newImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return newImage;
  }
}
