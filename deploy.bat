@ECHO off
FOR /F "tokens=* USEBACKQ" %%F IN (`where git`) DO (
SET git_path=%%F
)
"%git_path%\..\..\bin\bash.exe" deploy.sh
