import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Channel extends Val.Wrap<LinkedBlockingQueue<Val>> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Channel"))
      .put(new BuiltinFunc("__new__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return new Channel();
        }
      })
      .put(new BuiltinFunc("put") {
        public Val calli(Val self, ArrayList<Val> args)  {
          Err.expectArglen(args, 1);
          self.as(Channel.class, "self").put(args.get(0));
          return self;
        }
      })
      .put(new BuiltinFunc("take") {
        public Val calli(Val self, ArrayList<Val> args)  {
          Err.expectArglen(args, 0);
          return self.as(Channel.class, "self").take();
        }
      })
      .hm;

  public Channel() { super(new LinkedBlockingQueue<Val>()); }

  public void put(Val v) {
    try { val.put(v); }
    catch (InterruptedException e) { throw new Err(e); }
  }

  public Val take() {
    try { return val.take(); }
    catch (InterruptedException e) { throw new Err(e); }
  }

  public HashMap<String, Val> getMeta() { return MM; }

}
