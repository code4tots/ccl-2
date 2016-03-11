
func test() {
  runWithScope(globalScope,
      "print 'hey'\n" +
      "print 54\n" +
      "set x 55\n" +
      "print x\n" +
      "set f (fn [x] { just 5 })\n" +
      "print (f 4)\n" +
      "print f"
      ,
      "<main>")
}
