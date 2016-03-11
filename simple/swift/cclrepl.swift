
class CclRepl {
  var commandBuffer: String = ""
  var scope: Scope = Scope(globalScope)

  func processLine(line: String) {
    commandBuffer += line
    if !isPartial(commandBuffer) {
      runWithScope(scope, commandBuffer, "<stdin>")
      commandBuffer = ""
    }
  }
}

func isPartial(text: String) -> Bool {
  var count = 0
  for token in lex(text, "<stdin>") {
    if token.type == "{" {
      count++
    } else if token.type == "}" {
      count--
    }
  }
  return count > 0
}
