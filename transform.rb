paths = `ls src/**/*.java`.lines.map(&:strip)

paths.each do |path|
  File.write(path, "package com.ccl.core;\n\n" + File.read(path))
end
