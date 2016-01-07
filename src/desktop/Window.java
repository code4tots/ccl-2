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
      // 'put' and 'take' methods to imitate Channel.
      .put(new BuiltinFunc("gui#Window#put") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          self.as(Window.class, "self").commandChannel.put(args.get(0));
          return self;
        }
      })
      .put(new BuiltinFunc("gui#Window#take") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self.as(Window.class, "self").eventChannel.take();
        }
      })
      .hm;

  private final Frame frame;

  // Commands issued from CCL to Frame.
  // CCL code should never read from commandChannel.
  private final Channel commandChannel;

  // Events reported by Frame to CCL.
  // CCL code should never write to eventChannel.
  private final Channel eventChannel;

  // 'Window' is a singleton. I would have multiple, except
  // when any one of them closes, everyone is going to close.
  // TODO: Consider making them available anyway.
  private static Window instance = null;

  public static Window getInstance() {
    if (instance == null)
      instance = new Window();
    return instance;
  }

  private Window() {
    if (instance != null)
      throw new Err("Tried to create multiple instances of Window");

    this.commandChannel = new Channel();
    this.eventChannel = new Channel();
    this.frame = new Frame(commandChannel, eventChannel);
  }

  public HashMap<String, Val> getMeta() { return MM; }

  // All native Java gui code should live here.
  // The 'Window' class is just a thin 'Val' wrapper around this class.
  private static final class Frame {

    private final Channel commandChannel;
    private final Channel eventChannel;

    private final JFrame frame;
    private final JPanel panel;

    public Frame(
        final Channel commandChannel,
        final Channel eventChannel) {
      this.commandChannel = commandChannel;
      this.eventChannel = eventChannel;

      final JFrame[] frameptr = new JFrame[1];
      final JPanel[] panelptr = new JPanel[1];
      invokeAndWait(new Runnable() {
        public void run() {
          JFrame frame = frameptr[0] = new JFrame();
          JPanel panel = panelptr[0] = new JPanel();
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
          // I think only using DISPOSE_ON_CLOSE leaves around
          // some other swing threads for a second or two keeping
          // this alive.
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.setVisible(true);
        }
      });
      frame = frameptr[0];
      panel = panelptr[0];
      // Spawn the command processing thread.
      // This is where we read commands issued by CCL code and relay it to
      // native gui.
      Evaluator.go(new Runnable() {
        public void run() {
          Val cmd;
          // Keep on extracting events until you find the poison pill.
          while ((cmd = commandChannel.take()) != Nil.val) {
            try {
              // TODO: Perform command.
              System.out.println("Received command: " + cmd.toString());
            } catch (Throwable e) {
              // I can't die dammit! If I do, CCL code will no longer be able
              // to talk to the GUI!
              // TODO: Consider the alternatives.
              System.out.println(e);
            }
          }
          frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
      });
    }

    private static void invokeAndWait(Runnable r) {
      try { SwingUtilities.invokeAndWait(r); }
      catch (InterruptedException e) { throw new Err(e); }
      catch (InvocationTargetException e) { throw new Err(e); }
    }
  }

}
