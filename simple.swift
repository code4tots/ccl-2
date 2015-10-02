class Obj: Hashable {
  static var OBJECT_COUNT: Int = 0
  let uid: Int
  var attrs: [String: Obj] = [:]
  init() { uid = Obj.OBJECT_COUNT++ }
  func getMetaDict() -> Dict { fatalError("\(self.dynamicType) does not override getMetaDict") }
  func getattr(name: String) -> Obj {
    if let attr = attrs[name] {
      return attr
    }
    if let attr = getMetaDict().value[Str(name)] {
      if let attrf = attr as? Callable {
        return Method(self, attrf)
      }
      return attr
    }
    fatalError("Attribute \(name) not found")
  }
  // Make swift compiler happy
  var hashValue: Int { return 0 }
  func eq(rhs: Obj) -> Bool { return uid == rhs.uid }
}
func ==(lhs: Obj, rhs: Obj) -> Bool {
  return lhs.eq(rhs)
}
class Num: Obj {
  var value: Double
  init(_ value: Double) { self.value = value }
  override func eq(rhs: Obj) -> Bool {
    if let r = rhs as? Num { return r.value == value } else { return false }
  }
}
class Str: Obj {
  var value: String
  init(_ value: String) { self.value = value }
  override func eq(rhs: Obj) -> Bool {
    if let r = rhs as? Str { return r.value == value } else { return false }
  }
}
class List: Obj {
  var value: [Obj]
  init(_ value: [Obj]) { self.value = value }
  override func eq(rhs: Obj) -> Bool {
    if let r = rhs as? List { return r.value == value } else { return false }
  }
}
class Ast: Obj {
  let filespec: String
  let source: String
  let position: Int
  init(_ filespec: String, _ source: String, _ position: Int) {
    self.filespec = filespec
    self.source = source
    self.position = position
  }
  func eval(ctx: Dict) -> Obj {
    fatalError("\(self.dynamicType) does not override eval")
  }
}
class AstTemplate<T>: Ast {
  var value: T
  init(_ filespec: String, _ source: String, _ position: Int, _ value: T) {
    self.value = value
    super.init(filespec, source, position)
  }
}
class AstObjTemplate<T: Obj>: AstTemplate<T> {
  override init(_ filespec: String, _ source: String, _ position: Int, _ value: T) {
    super.init(filespec, source, position, value)
  }
  override func eq(rhs: Obj) -> Bool {
    if let r = rhs as? AstTemplate<T> { return r.value == value } else { return false }
  }
}
class NumAst: AstObjTemplate<Num> {
  override init(_ filespec: String, _ source: String, _ position: Int, _ value: Num) {
    super.init(filespec, source, position, value)
  }
  override func eval(ctx: Dict) -> Obj {
    return value
  }
}
class StrAst: AstObjTemplate<Str> {
  override init(_ filespec: String, _ source: String, _ position: Int, _ value: Str) {
    super.init(filespec, source, position, value)
  }
  override func eval(ctx: Dict) -> Obj {
    return value
  }
}
class ListAst: AstTemplate<[Ast]> {
  override init(_ filespec: String, _ source: String, _ position: Int, _ value: [Ast]) {
    super.init(filespec, source, position, value)
  }
  override func eval(ctx: Dict) -> Obj {
    if value.count == 0 {
      fatalError("Tried to evaluate an empty list")
    }
    let fobj = value[0].eval(ctx)
    if let f = fobj as? Invocable {
      return f.invoke(ctx, Array(value[0..<(value.count-1)]))
    }
    fatalError("\(fobj) is not callable")
  }
  override func eq(rhs: Obj) -> Bool {
    if let r = rhs as? ListAst { return r.value == value } else { return false }
  }
}
class Dict: Obj {
  var value: [Obj: Obj]
  init(_ value: [Obj: Obj]) { self.value = value }
}
class Invocable: Obj {
  func invoke(ctx: Dict, _ args: [Ast]) -> Obj {
    fatalError("\(self.dynamicType) does not override invoke")
  }
}
class Callable: Invocable {
  override func invoke(ctx: Dict, _ args: [Ast]) -> Obj {
    return call(args.map({ $0.eval(ctx) }))
  }
  func call(args: [Obj]) -> Obj {
    fatalError("\(self.dynamicType) does not override call")
  }
}
class Method: Callable {
  let owner: Obj
  let callable: Callable
  init(_ owner: Obj, _ callable: Callable) { self.owner = owner; self.callable = callable }
  override func call(args: [Obj]) -> Obj {
    return callable.call([owner] + args)
  }
}
func isspace(c: Character) -> Bool {
  return c == " " || c == "\n" || c == "\t"
}
func isdigit(c: Character) -> Bool {
  return c == "0" || c == "1" || c == "2" || c == "3" || c == "4" ||
         c == "5" || c == "6" || c == "7" || c == "8" || c == "9"
}
func isword(c: Character) -> Bool {
  return true
}
func getLineCount(t: [Character], _ i: Int) -> Int {
  return t[0..<i].reduce(1, combine: { $0 + ($1 == "\n" ? 1 : 0) })
}
func startswith(t: [Character], _ qchar: Character, _ qcnt: Int, _ i: Int) -> Bool {
  for di in 0..<qcnt {
    if i+di >= t.count || t[i+di] != qchar {
      return false
    }
  }
  return true
}
func parse(fs: String, s: String) -> ListAst {
  var i: Int = 0
  var stack: [ListAst] = []
  let t = Array(s.characters)
  stack.append(ListAst(fs, s, 0, [StrAst(fs, s, 0, Str("begin"))]))
  while true {
    while i < t.count && isspace(t[i]) {
      i++
    }
    if i >= t.count {
      break
    }
    let j = i
    let c = t[i]

    if c == "(" {
      i++
      stack.append(ListAst(fs, s, j, []))
      continue
    }

    if c == ")" {
      if t.count == 1 {
        let glc = getLineCount(t, i)
        fatalError("Close parenthesis without matching open parenthesis on line: \(glc)")
      }
      i++
      let list = stack.removeLast()
      stack.last!.value.append(list)
      continue
    }

    // String literal
    var raw = false
    if c == "r" {
      raw = true
      i++
    }

    if i < t.count && (t[i] == "\"" || t[i] == "'") {
      let qchar: Character = t[i]
      let qcnt: Int = (i+2 < t.count && t[i+1] == qchar && t[i+2] == qchar) ? 3 : 1
      var sb: String = ""
      i += qcnt
      while i < t.count && !startswith(t, qchar, qcnt, i) {
        let d = t[i]
        if !raw && d == "\\" {
          if i >= t.count {
            fatalError("Last character of your program is \\ and your string is unterminated")
          }
          i++
          let e = t[i]
          switch e {
          case "\\": sb.append("\\" as Character)
          case "n": sb.append("n" as Character)
          default: fatalError("Unrecognized string escape: \(e)")
          }
        }
      }
      if i >= t.count {
        fatalError("You didn't finish your quotes")
      }
      i += qcnt
      stack.last!.value.append(StrAst(fs, s, j, Str(sb)))
      continue
    }
    else {
      i = j
    }

    // Number
    if c == "-" {
      i++
    }
    var seenDot = false
    if i < t.count && t[i] == "." {
      seenDot = true
      i++
    }
    if i < t.count && isdigit(t[i]) {
      while i < t.count && isdigit(t[i]) {
        i++
      }
      if (!seenDot) {
        if i < t.count && t[i] == "." {
          i++
          while i < t.count && isdigit(t[i]) {
            i++
          }
        }
      }
      stack.last!.value.append(NumAst(fs, s, j, Num(Double(String(t[j..<i]))!)))
      continue
    }

    // Name
    while i < t.count && isword(t[i]) {

    }
  }
  return stack[0]
}
