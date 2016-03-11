//
//  cclcore.swift
//  ccl
//
//  Created by Kyumin Kim on 3/10/16.
//  Copyright © 2016 Kyumin Kim. All rights reserved.
//

// ******************************************************************
// ******************************************************************
// ******************************************************************
// ******************************************************************
// ******************************************************************
// ******************************************************************

// You probably don't want to edit anything in this file.
// Except maybe to clean up and do bug fixes.

// I'm hoping that this is pretty low level core of the language
// that I won't have to edit that often.
// This file covers, lexing, parsing, variable name resolution,
// ccl types and ast evaluation.

// For extending the language with macros and builtin functions,
// see cclscope.swift.

// For adding tests, see ccltest.swift

// ******************************************************************
// ******************************************************************
// ******************************************************************
// ******************************************************************



//: Playground - noun: a place where people can play

func isspace(c: Character) -> Bool {
    return c == " " || c == "\t"
}

func isword(c: Character) -> Bool {
    return c == "_" || (c >= "0" && c <= "9") || (c >= "a" && c <= "z") || (c >= "A" && c <= "Z")
}

func isdig(c: Character) -> Bool {
    return c >= "0" && c <= "9"
}

@noreturn func die(msg: String) {
    preconditionFailure(msg)
}

struct Token {
    let source: Source
    let type: String
    let i: String.Index
    let value: String
}

class Source {
    let text: String
    let filespec: String
    init(text: String, filespec: String) {
        self.text = text
        self.filespec = filespec
    }
}

func ==(lhs: Value, rhs: Value) -> Bool {
    return lhs.eq(rhs)
}

class Value: Hashable, CustomStringConvertible {
    var hashValue: Int {
        die("hashValue not implemented")
    }
    func eq(other: Value) -> Bool {
        die("Didn't override eq")
    }
    var description: String {
        return "<Value -- \(self.dynamicType) (unimplemented)>"
    }
}

class Scope {
    let parent: Scope?
    var table: [String: Value] = [:]
    
    init(_ parent: Scope?) {
        self.parent = parent
    }
    
    subscript(index: String) -> Value {
        get {
            if let value = table[index] {
                return value
            }
            if let p = parent {
                return p[index]
            }
            die("No such name '\(index)'")
        }
        set(value) {
            table[index] = value
        }
    }
}

class Niil: Value {
    override var hashValue: Int {
        return 97
    }
    
    override var description: String {
        return "nil"
    }
}

class Number: Value {
    var value = 0.0
    
    init(_ value: Double) {
        self.value = value
    }
    
    override var hashValue: Int {
        return value.hashValue
    }
    
    override func eq(other: Value) -> Bool {
        if let x = other as? Number {
            return value == x.value
        }
        return false
    }
    
    override var description: String {
        return value.description
    }
}

class Text: Value {
    var value = ""
    
    init(_ value: String) {
        self.value = value
    }
    
    override var hashValue: Int {
        return value.hashValue
    }
    
    override func eq(other: Value) -> Bool {
        if let x = other as? Text {
            return value == x.value
        }
        return false
    }
    
    override var description: String {
        return value
    }
}

class List: Value {
  var value: [Value] = []

  init(_ value: [Value]) {
    self.value = value
  }

  override var hashValue: Int {
    return value.count // TODO
  }

  override func eq(other: Value) -> Bool {
    if let x = other as? List {
      return value == x.value
    }
    return false
  }
}

class Table: Value {
    var value: [Value: Value] = [:]
    
    override var hashValue: Int {
        return value.count // TODO
    }
    
    override func eq(other: Value) -> Bool {
        if let x = other as? Table {
            return value == x.value
        }
        return false
    }
}

class Ast {
    let token: Token
    
    init(_ token: Token) {
        self.token = token
    }
    
    func eval(scope: Scope) -> Value {
        die("eval not implemented")
    }
}

class Callable: Value {
    func call(scope: Scope, args: [Ast]) -> Value {
        die("Not implemented")
    }
}

class SpecialForm: Callable {
    
    let block: (Scope, [Ast]) -> Value
    
    init(_ block: (Scope, [Ast]) -> Value) {
        self.block = block
    }
    
    override func call(scope: Scope, args: [Ast]) -> Value {
        return block(scope, args)
    }
}

class BaseFunction: Callable {
    override final func call(scope: Scope, args: [Ast]) -> Value {
        var eargs: [Value] = []
        for arg in args {
            eargs.append(arg.eval(scope))
        }
        return callf(eargs)
    }
    
    func callf(args: [Value]) -> Value {
        die("Not implemented")
    }
}

class Function: BaseFunction {
    
  let scope: Scope
  let args: [String]
  let body: Ast
  
  init(_ scope: Scope, _ args: [String], _ body: Ast) {
    self.scope = scope
    self.args = args
    self.body = body
  }
  
  override func callf(args: [Value]) -> Value {
    let scope = Scope(self.scope)
    for (name, value) in zip(self.args, args) {
      scope[name] = value
    }
    return body.eval(scope)
  }
}

class BuiltinFunction: BaseFunction {
    let block: ([Value]) -> Value
    
    init(_ block: ([Value]) -> Value) {
        self.block = block
    }
    
    override func callf(args: [Value]) -> Value {
        return block(args)
    }
}

func lex(text: String, _ filespec: String) -> [Token] {
  var i = text.startIndex
  let source = Source(text: text, filespec: filespec)
  var tokens: [Token] = []
  while true {
    while i < text.endIndex && isspace(text[i]) {
      i++
    }
    
    if i >= text.endIndex {
      break
    }
    
    // Special characters
    if ["\n", "(", ")", "{", "}", "[", "]"].contains(text[i]) {
      tokens.append(Token(source: source, type: String(text[i]), i: i, value: ""))
      i++
      continue
    }
    
    let j = i
    
    // STR
    if text[i] == "'" || text[i] == "\"" ||
        text[i..<text.endIndex].hasPrefix("r\"\"\"") ||
        text[i..<text.endIndex].hasPrefix("r'''") {
      var raw = false
      if text[i] == "r" {
        raw = true
        i++
      }
      var quotes = String(text[i])
      if text[i..<text.endIndex].hasPrefix("r\"\"\"") ||
          text[i..<text.endIndex].hasPrefix("r'''") {
        quotes = text[i...i.successor().successor()]
        i = i.successor().successor().successor()
      } else {
        i++
      }
      var sb = ""
      while i < text.endIndex && !text[i..<text.endIndex].hasPrefix(quotes) {
        if raw || text[i] != "\\" {
          sb.append(text[i])
          i++
        } else {
          i++
          if i >= text.endIndex {
            die("Started string escape but found EOF")
          }
          switch text[i] {
          case "n":
            sb += "\n"
          case "t":
            sb += "\t"
          default:
            die("Unrecognized string escape: \(text[i])")
          }
        }
      }
      if i >= text.endIndex {
        die("Unterminated string literal")
      }
      i = i.advancedBy(quotes.characters.count)
      tokens.append(Token(source: source, type: "STR", i: j, value: sb))
      continue
    }

    // NUM
    if isdig(text[i]) || text[i] == "-" || text[i] == "." {
      var seenDigit = false
      if text[i] == "-" {
        i++
      }
      while i < text.endIndex && isdig(text[i]) {
        seenDigit = true
        i++
      }
      if i < text.endIndex && text[i] == "." {
        i++
      }
      while i < text.endIndex && isdig(text[i]) {
        seenDigit = true
        i++
      }
      if !seenDigit {
        die("Not a valid number: " + text[j..<i])
      }
      tokens.append(Token(source: source, type: "NUM", i: j, value: text[j..<i]))
      continue
    }

    // ID
    while i < text.endIndex && (text[i] == "_" || isword(text[i])) {
      i++
    }

    if i != j {
      tokens.append(Token(source: source, type: "ID", i: j, value: text[j..<i]))
      continue
    }

    die("Invalid token: " + text[i..<text.endIndex])
  }
  tokens.append(Token(source: source, type: "\n", i: i, value: ""))
  tokens.append(Token(source: source, type: "EOF", i: i, value: ""))
  return tokens
}

class Literal: Ast {
  let value: Value

  init(_ token: Token, _ value: Value) {
    self.value = value
    super.init(token)
  }

  override func eval(scope: Scope) -> Value {
    return value
  }
}

class Name: Ast {
  let name: String

  init(_ token: Token, _ name: String) {
    self.name = name
    super.init(token)
  }

  override func eval(scope: Scope) -> Value {
    return scope[name]
  }
}

class Command: Ast {
  let f: Ast
  let args: [Ast]

  init(_ token: Token, _ f: Ast, _ args: [Ast]) {
    self.f = f
    self.args = args
    super.init(token)
  }

  override func eval(scope: Scope) -> Value {
    if let ff = f.eval(scope) as? Callable {
      return ff.call(scope, args: args)
    }
    die("Tried to call something that wasn't callable")
  }
}

class SpecialCommand: Ast {
  let f: Callable
  let args: [Ast]

  init(_ token: Token, _ f: Callable, _ args: [Ast]) {
    self.f = f
    self.args = args
    super.init(token)
  }

  override func eval(scope: Scope) -> Value {
    return f.call(scope, args: args)
  }
}

class Parser {
  let tokens: [Token]
  var i = 0

  init(_ tokens: [Token]) {
    self.tokens = tokens
  }

  func peek() -> Token {
    return tokens[i]
  }

  func next() -> Token {
    let token = peek()
    i++
    return token
  }

  func at(type: String) -> Bool {
    return peek().type == type
  }

  func consume(type: String) -> Bool {
    if at(type) {
      i++
      return true
    }
    return false
  }

  func expect(type: String) -> Token {
    if at(type) {
      return next()
    }
    die("Expected \(type) but found \(tokens[i].type)")
  }

  func parseAtom() -> Ast {
    let token = peek()
    if at("(") {
      return parseParentheticalCommand()
    } else if at("ID") {
      return Name(token, next().value)
    } else if at("NUM") {
      return Literal(token, Number(Double(next().value)!))
    } else if at("STR") {
      return Literal(token, Text(next().value))
    } else if consume("{") {
      var args: [Ast] = []
      while consume("\n") {}
      while !consume("}") {
          args.append(parseCommand())
      }
      return SpecialCommand(token, specialFormBlock, args)
    } else if consume("[") {
      var args: [Ast] = []
      while consume("\n") {}
      while !consume("]") {
          args.append(parseAtom())
          while consume("\n") {}
      }
      return SpecialCommand(token, builtinFunctionList, args)
    }

    die("Not valid atom: " + peek().type)
  }

  func parseCommand() -> Command {
    let token = peek()
    let f = parseAtom()
    var args: [Ast] = []
    while !at("\n") && !at("}") {
      args.append(parseAtom())
    }
    while consume("\n") {}
    return Command(token, f, args)
  }
  
  func parseParentheticalCommand() -> Command {
    expect("(")
    let token = peek()
    let f = parseAtom()
    var args: [Ast] = []
    while consume("\n") {}
    while !consume(")") {
      args.append(parseAtom())
      while consume("\n") {}
    }
    return Command(token, f, args)
  }
  
  func parseProgram() -> Ast {
    let token = peek()
    var args: [Ast] = []
    while consume("\n") {}
    while !at("EOF") {
      args.append(parseCommand())
    }
    return SpecialCommand(token, specialFormBlock, args)
  }
}

func parse(text: String, _ filespec: String) -> Ast {
  return Parser(lex(text, filespec)).parseProgram()
}

func runWithScope(scope: Scope, _ text: String, _ filespec: String) {
  parse(text, filespec).eval(scope)
}
