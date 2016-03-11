//
//  cclscope.swift
//  ccl
//
//  Created by Kyumin Kim on 3/10/16.
//  Copyright © 2016 Kyumin Kim. All rights reserved.
//

// ---- Global scope stuff ---

let niil = Niil()

let specialFormBlock = SpecialForm({ (scope: Scope, args: [Ast]) -> Value in
  var last: Value = niil
  for arg in args {
    last = arg.eval(scope)
  }
  return last
})

let builtinFunctionPrint = BuiltinFunction({ (args: [Value]) -> Value in
  print("\(args[0])")
  return niil
})

let builtinFunctionList = BuiltinFunction({ (args: [Value]) -> Value in
  return List(args)
})

let builtinFunctionJust = BuiltinFunction({ (args: [Value]) -> Value in
  return args[0]
})

let specialFormFn = SpecialForm({ (scope: Scope, args: [Ast]) -> Value in
  let argnames = (args[0] as! SpecialCommand).args.map({ (n: Ast) -> String in
    (n as! Name).name
  })
  return Function(scope, argnames, args[1])
})

let specialFormSet = SpecialForm({ (scope: Scope, args: [Ast]) -> Value in
  let name = (args[0] as! Name).name
  let value = args[1].eval(scope)
  scope[name] = value
  return value
})

func makeGlobalScope() -> Scope {
  let globalScope = Scope(nil)
  globalScope["__block__"] = specialFormBlock
  globalScope["nil"] = niil
  globalScope["print"] = builtinFunctionPrint
  globalScope["fn"] = specialFormFn
  globalScope["set"] = specialFormSet
  globalScope["just"] = builtinFunctionJust
  return globalScope
}

let globalScope = makeGlobalScope()
