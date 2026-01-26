@echo off

mkdir target\distribution
mkdir target\distribution-libs
xcopy /s /q target\libs\* target\distribution-libs
copy target\%MAIN_JAR% target\distribution-libs

echo jdeps
"%JAVA_HOME%\bin\jdeps" ^
  -q ^
  --multi-release %JAVA_VERSION% ^
  --ignore-missing-deps ^
  --print-module-deps ^
  --class-path "target\distribution-libs\*" ^
  "target\%MAIN_JAR%" > target\jdeps.txt
set /p detected_modules=<target\jdeps.txt
echo detected modules: %detected_modules%

set manual_modules=jdk.security.auth,jdk.security.jgss,jdk.crypto.ec,jdk.localedata
echo manual modules: %manual_modules%

echo jlink
call "%JAVA_HOME%\bin\jlink" ^
  --strip-native-commands ^
  --no-header-files ^
  --no-man-pages ^
  --strip-debug ^
  --add-modules %detected_modules%,%manual_modules% ^
  --include-locales=en ^
  --output target/java-runtime

call :jpackage exe
call :jpackage msi

echo portable
"distribution\win\soft\lessmsi\lessmsi.exe" x "target\distribution\prafka-%PROJECT_VERSION%.msi" "target\distribution\"
"distribution\win\soft\7za.exe" a -tzip "target\distribution\prafka-%PROJECT_VERSION%.zip" ".\target\distribution\SourceDir\Prafka\*"
rmdir /s /q target\distribution\SourceDir

echo checksums
for %%f in (target\distribution\*.exe target\distribution\*.msi target\distribution\*.zip) do (
    powershell -Command "Get-FileHash -Path '%%f' -Algorithm SHA256 | Select-Object -ExpandProperty Hash | Out-File -FilePath '%%f.sha256' -Encoding ASCII"
)

exit \b %ERRORLEVEL%

:jpackage
    set distribution_type=%~1

    echo jpackage %distribution_type%
    call "%JAVA_HOME%\bin\jpackage" ^
      --type %distribution_type% ^
      --dest target/distribution ^
      --input target/distribution-libs ^
      --runtime-image target/java-runtime ^
      --main-class %MAIN_CLASS% ^
      --main-jar %MAIN_JAR% ^
      --java-options "%JAVA_OPTIONS%" ^
      --name "%PROJECT_NAME%" ^
      --description "%PROJECT_DESCRIPTION%" ^
      --app-version %PROJECT_VERSION% ^
      --vendor "%VENDOR_NAME%" ^
      --about-url "%VENDOR_URL%" ^
      --icon distribution/win/icon.ico ^
      --win-dir-chooser ^
      --win-menu ^
      --win-shortcut ^
      --win-help-url "%VENDOR_URL%"

    ren "target\distribution\%PROJECT_NAME%-%PROJECT_VERSION%.%distribution_type%" ^
        "prafka-%PROJECT_VERSION%.%distribution_type%"
exit /b 0