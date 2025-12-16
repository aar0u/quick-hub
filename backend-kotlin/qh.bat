chcp 65001
@echo off

@REM copy hub.bat D:\user-bin\ /y

if "%~1"=="" (
    echo Usage: hub.bat [folder] [http_port]
    echo Example: hub.bat D:\myfolder 8080
    exit /b 1
)

set folder=%~1
set http_port=%~2
if "%http_port%"=="" set http_port=3006

rem Calculate HTTPS port by adding 363 to HTTP port
set /a https_port=%http_port%+363

java -jar D:\dev\quick-hub\backend-kotlin\build\libs\quick-hub.jar %folder%