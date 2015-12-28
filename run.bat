@echo off
if not exist cls mkdir cls
REM "Even though cmd.exe doesn't expand wildcards, java/javac"
REM "seem to do it for us."
javac -Xlint src\*.java -Xdiags:verbose -d cls && java -cp cls SimpleDesktop %1
