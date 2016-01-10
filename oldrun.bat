REM "This is dated, but I haven't had a chance to test on a Windows"
REM "machine recently, so haven't been able to update it."

@echo off
if not exist cls mkdir cls
REM "Even though cmd.exe doesn't expand wildcards, java/javac"
REM "seem to do it for us."
javac -Xlint src\*.java -Xdiags:verbose -d cls && java -cp cls SimpleDesktop %1
