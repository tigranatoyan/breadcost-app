@echo off
REM ============================================================
REM  BreadCost — Start Backend + Frontend
REM  Run from the project root: start.bat
REM ============================================================

echo === BreadCost Startup ===
echo.

REM ── Start Backend (Spring Boot) in a new window ──
REM   Default: PostgreSQL (requires docker-compose up -d)
REM   Dev mode (H2): set SPRING_PROFILES_ACTIVE=dev before running
echo [1/2] Starting Backend on http://localhost:8080 ...
if "%SPRING_PROFILES_ACTIVE%"=="" (
    echo       Profile: default (PostgreSQL — run 'docker-compose up -d' first)
) else (
    echo       Profile: %SPRING_PROFILES_ACTIVE%
)
start "BreadCost-Backend" cmd /k "cd /d %~dp0 && .\gradlew bootRun"

REM ── Wait a few seconds for backend to initialize ──
echo       Waiting 10s for backend to start...
timeout /t 10 /nobreak >nul

REM ── Start Frontend (Next.js) in a new window ──
echo [2/2] Starting Frontend on http://localhost:3000 ...
start "BreadCost-Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo === Both servers starting ===
echo   Backend:  http://localhost:8080  (Spring Boot + H2)
echo   Frontend: http://localhost:3000  (Next.js)
echo.
echo   Login:    admin / admin
echo   H2 DB:    http://localhost:8080/h2-console
echo.
echo Close this window anytime. The servers run in their own windows.
pause
