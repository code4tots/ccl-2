//
//  main.swift
//  ccl
//
//  Created by Kyumin Kim on 3/10/16.
//  Copyright © 2016 Kyumin Kim. All rights reserved.
//

func input(prompt: String) -> String? {
  print(prompt, terminator: "")
  return readLine()
}

import Foundation

if Process.arguments.count == 1 {
  // If there's no arguments, start the REPL
  let repl = CclRepl()
  while let line = input(repl.commandBuffer == "" ? ">>> " : "... ") {
    repl.processLine(line)
  }
} else if Process.arguments.count == 2 {
  // If user provides a script, run that instead.
  var text: String
  do {
    // This requires 'Foundation'
    text = try String(contentsOfFile: Process.arguments[1])
  } catch {
    text = ""
    die("Could not open file \(Process.arguments[1])")
  }
  runWithScope(globalScope, text, Process.arguments[1])
}
