@echo off
echo ========================================
echo FastChat Backend Build Test
echo ========================================

echo.
echo 1. Testing Maven wrapper...
if exist "mvnw.cmd" (
    echo Maven wrapper found
) else (
    echo Maven wrapper NOT found!
    exit /b 1
)

echo.
echo 2. Cleaning and compiling...
call mvnw.cmd clean compile -DskipTests
if errorlevel 1 (
    echo COMPILATION FAILED!
    exit /b 1
)

echo.
echo 3. Running package (skip tests)...
call mvnw.cmd package -DskipTests
if errorlevel 1 (
    echo PACKAGING FAILED!
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo You can now deploy or run the application with:
echo java -jar target\fastchat-backend-0.0.1-SNAPSHOT.jar