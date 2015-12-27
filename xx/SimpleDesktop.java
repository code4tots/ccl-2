import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class SimpleDesktop extends Simple {

public static void main(String[] args) {
  new SimpleDesktop().xmain(args);
}

public void xmain(String[] args) {
  ModuleAst mainModule = readModule(args[0]);
  run(mainModule, "__main__");
}

public static String readFile(String path) {
  try {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line = null;
    StringBuilder sb = new StringBuilder();
    String separator = System.getProperty("line.separator");

    while((line = reader.readLine()) != null) {
      sb.append(line);
      sb.append(separator);
    }

    return sb.toString();
  } catch (IOException e) {
    throw new RuntimeException(
        "Exception while reading " + path + ": " + e.toString());
  }
}

public ModuleAst readModule(String path) {
  return new Parser(readFile(path), path).parse();
}

}
