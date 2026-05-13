@echo off
setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0"

if "%SONAR_HOST_URL%"=="" (
    set "SONAR_HOST_URL=http://localhost:9000"
)

if "%MAVEN_GOALS%"=="" (
    set "MAVEN_GOALS=verify sonar:sonar"
)

if "%SONAR_TOKEN%"=="" (
    echo SONAR_TOKEN is not set. Generate a token in SonarQube and set SONAR_TOKEN before running scans.
    exit /b 1
)

set "FAILED_SERVICES="
set "SERVICES=api-gateway auth category commentservice media notification post service-registry"

for %%S in (%SERVICES%) do (
    echo.
    echo ==================================================
    echo Scanning %%S
    echo ==================================================
    pushd "%ROOT_DIR%%%S" >nul
    call mvn %MAVEN_GOALS% -Dsonar.host.url="%SONAR_HOST_URL%" -Dsonar.token="%SONAR_TOKEN%"
    if errorlevel 1 (
        echo SonarQube scan failed for %%S
        set "FAILED_SERVICES=!FAILED_SERVICES! %%S"
    )
    popd >nul
)

echo.
if not "%FAILED_SERVICES%"=="" (
    echo The following services failed:%FAILED_SERVICES%
    exit /b 1
)

echo All SonarQube scans completed successfully.
exit /b 0
