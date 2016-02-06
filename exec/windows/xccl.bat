@echo off

REM Ugh. In windows there doesn't seem to be an easy way around listing out
REM every directory.
REM Also, there might be issues if you have spaces in path name...

if [%1]==[] goto usage

SET root=%~dp0\..\..
if not exist %root%\cls mkdir %root%\cls
javac -Xlint -Xdiags:verbose -g %root%\src\val\*.java %root%\src\desktop\*.java %root%\src\eval\*.java %root%\src\grammar\*.java %root%\src\translator\*.java %root%\src\val\*.java -d %root%\cls || goto fail_to_compile
java -cp %root%\cls com.ccl.core.Desktop %root% %1
goto :eof

:usage
@echo Usage: %0 ^<path_to_module^>
exit /B 1

:fail_to_compile
@echo Compilation failed
exit /B 1
