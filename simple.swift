class Obj: Hashable {
  static var objcnt: Int = 0

  let uid: Int

  init() {
    uid = Obj.objcnt++
  }

  func callForm(ctx: Dict, args: List, inout result: Obj, inout err: Obj) -> Obj? {
    return Str("callForm is not supported for \(self.dynamicType)")
  }

  func call(args: List, inout result: Obj, inout err: Obj) -> Obj? {
    return Str("call is not supported for \(self.dynamicType)")
  }

  var hashValue: Int {
    return 0
  }
}

func ==(lhs: Obj, rhs: Obj) -> Bool {
  return false
}

class Num: Obj {
  let value: Double
  init(_ value: Double) { self.value = value }
}

class Str: Obj {
  let value: String
  init(_ value: String) { self.value = value }
}

class List: Obj {
  var value: [Obj]
  init(_ value: [Obj]) { self.value = value }
}

class Dict: Obj {
  var value: [Obj: Obj]
  init(_ value: [Obj: Obj]) { self.value = value }
}

class Origin: Obj {
  let filespec: String
  let source: String
  let position: Int
  init(_ filespec: String, _ source: String, _ position: Int) {
    self.filespec = filespec
    self.source = source
    self.position = position
  }
}

class Err: Obj {
  let message: String
  var trace: [Origin] = []
  init(_ message: String) { self.message = message }
}

class Ast: Obj {
  func eval(ctx: Dict, inout result: Obj, inout err: Obj) -> Obj? {
    return Str("eval should be implemented... but wasn't")
  }
}

class NumAst: Ast {
}

print("hi")
