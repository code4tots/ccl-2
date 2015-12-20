@echo off
if not exist cls mkdir cls
REM "Even though cmd.exe doesn't expand wildcards, java/javac"
REM "seem to do it for us."
"C:\Program Files\Java\jdk1.8.0_65\bin\javac.exe" -Xlint src\*.java -Xdiags:verbose -d cls
"C:\Program Files\Java\jdk1.8.0_65\bin\java.exe" -cp cls Sanity %1 src\*.ccl
