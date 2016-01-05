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
          try { self.as(Channel.class, "self").val.put(args.get(0)); }
          catch (InterruptedException e) { throw new Err(e); }
          return self;
        }
      })
      .put(new BuiltinFunc("take") {
        public Val calli(Val self, ArrayList<Val> args)  {
          Err.expectArglen(args, 0);
          try { return self.as(Channel.class, "self").val.take(); }
          catch (InterruptedException e) { throw new Err(e); }
        }
      })
      .hm;

  public Channel() { super(new LinkedBlockingQueue<Val>()); }

  public HashMap<String, Val> getMeta() { return MM; }

}
