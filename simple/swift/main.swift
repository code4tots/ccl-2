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

if Process.arguments.count == 1 {
  // If there's no arguments, start the REPL
  let repl = CclRepl()
  while let line = input(repl.commandBuffer == "" ? ">>> " : "... ") {
    repl.processLine(line)
  }
}
