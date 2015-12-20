@echo off
if not exist cls mkdir cls
"C:\Program Files\Java\jdk1.8.0_65\bin\javac.exe" -Xlint src\*.java -d cls
"C:\Program Files\Java\jdk1.8.0_65\bin\java.exe" -cp cls Sanity %1 src\*.ccl
