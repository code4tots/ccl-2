package com.ccl;

public class Modules extends Runtime {

  public static void main(String[] args) {
    import_test();
  }
  
  private static Blob module_test = null;
  public static Value import_test() {
    if (module_test == null) {
      module_test = newModuleScope();
      run_test(module_test);
    }
    return module_test;
  }
  private static void run_test(Blob scope) {
    Value condition;
  }
}
