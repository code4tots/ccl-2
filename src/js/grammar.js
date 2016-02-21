"use strict"
// grammar.js
// CCL grammar.

/// Lexer

var KEYWORDS = [
  'and', 'or', 'is',
  'if', 'else', 'while', 'not', 'break', 'continue',
  'return'
]

var SYMBOLS = [
  '(', ')', '[', ']', '{', '}', '.', ':', ',', '@',
  '=', '==', '!=', '<', '<=', '>', '>=',
  '+', '-', '*', '/', '//', '%', '\\', '\\\\'
]

function Token(source, i, type, value) {
  this.source = soruce
  this.i = i
  this.type = type
  this.value = value
}

Token.prototype = {
  getLineNumber: function() {
    var lc = 1
    for (var j = 0; j < i; j++) {
      if (this.source.string[0] === '\n') {
        lc++
      }
    }
    return lc
  },
  getLocationString: function() {
    var filespec = this.source.filespec
    var lineno = this.getLineNumber()
    return 'in file "' + filespec + '" on line ' + lineno
  }
}

function lex(string, filespec) {
  var source = {string: string, filespec: filespec}
  var i = 0
  var peek = null
  var done = false
  var tokens = []

  return tokens
}
