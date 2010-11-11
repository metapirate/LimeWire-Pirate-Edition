@echo off

SETLOCAL ENABLEDELAYEDEXPANSION 
SET PATH=%PATH%;lib\native\windows
SET CLASSPATH=lib\resources\windows

REM Get all common jars.
FOR %%j IN (lib\jars\*.jar) DO (
  SET CLASSPATH=!CLASSPATH!;%%j
)

REM Get all other jars.
FOR %%j IN (lib\jars\other\*.jar) DO (
  SET CLASSPATH=!CLASSPATH!;%%j
)

REM Get all windows jars.
FOR %%j IN (lib\jars\windows\*.jar) DO (
  SET CLASSPATH=!CLASSPATH!;%%j
)

REM Get all components
FOR /D %%c IN (components\*) DO (
  IF EXIST %%c\src (
    SET CLASSPATH=!CLASSPATH!;%%c\build\classes;%%c\src\main\resources
  )
)

FOR /D %%c IN (private-components\*) DO (
  IF EXIST %%c\src (
    SET CLASSPATH=!CLASSPATH!;%%c\build\classes;%%c\src\main\resources
  )
)


java -Djava.net.preferIPV6Addresses=false -ea -da:ca.odell.glazedlists... -Djava.net.preferIPv4stack=true -Djna.library.path=lib\native\windows org.limewire.ui.swing.Main