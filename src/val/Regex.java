package com.ccl.core;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class Regex extends Val.Wrap<Pattern> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Regex"))
      .put(new BuiltinFunc("Regex#__new__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return Regex.from(args.get(0).as(Str.class, "arg").val);
        }
      })
      .put(new BuiltinFunc("Regex#str") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Str.from(self.as(Regex.class, "self").val.pattern());
        }
      })
      .put(new BuiltinFunc("Regex#repl") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArgRange(args, 2, 4);
          Pattern p = self.as(Regex.class, "self").val;
          String init = args.get(0).as(Str.class, "argument 0").val;
          String repl = args.get(1).as(Str.class, "argument 1").val;
          int start = args.size() > 2 ?
              args.get(2).as(Num.class, "argument 2").asIndex(): 0;
          int end = args.size() > 3 ?
              args.get(3).as(Num.class, "argument 3").asIndex():
              init.length();
          Matcher m = p.matcher(init);
          StringBuffer sb = new StringBuffer();
          m.region(start, end);
          m.useAnchoringBounds(false);
          m.useTransparentBounds(true);
          while (m.find()) {
            m.appendReplacement(sb, repl);
          }
          m.appendTail(sb);
          return Str.from(sb.toString());
        }
      })
      .put(new BuiltinFunc("Regex#find") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArgRange(args, 1, 3);
          Pattern p = self.as(Regex.class, "self").val;
          String init = args.get(0).as(Str.class, "argument 0").val;
          int start = args.size() > 1 ?
              args.get(1).as(Num.class, "argument 1").asIndex(): 0;
          int end = args.size() > 2 ?
              args.get(2).as(Num.class, "argument 2").asIndex():
              init.length();
          Matcher m = p.matcher(init);
          m.region(start, end);
          m.useAnchoringBounds(false);
          m.useTransparentBounds(true);

          if (!m.find())
            return Nil.val;

          int gc = m.groupCount();
          ArrayList<Val> groups = new ArrayList<Val>();
          for (int i = 0; i <= gc; i++) {
            String gr = m.group(i);
            if (gr == null)
              groups.add(Nil.val);
            else
              groups.add(Str.from(gr));
          }

          return List.from(groups);
        }
      })
      .hm;

  private Regex(String pattern) {
    super(Pattern.compile(pattern, Pattern.MULTILINE|Pattern.DOTALL));
  }
  public static Regex from(String pattern) { return new Regex(pattern); }
  public HashMap<String, Val> getMeta() { return MM; }
}
