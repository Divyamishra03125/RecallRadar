@echo off
echo ========================================================
echo Starting RecallRadar Full Stack (Frontend + Backend)
echo ========================================================

echo [1/3] Checking PostgreSQL service...
net start postgresql-x64-18 >nul 2>&1
if %errorlevel% neq 0 (
    echo Note: If PostgreSQL did not start, please start postgresql-x64-18 via services.msc or Admin CMD.
) else (
    echo PostgreSQL service is running.
)

echo [2/3] Starting Spring Boot Backend on http://localhost:8080 ...
start "RecallRadar Backend" cmd /k "cd /d "%~dp0recallradar-backend" && java -jar target\recallradar-backend-0.0.1-SNAPSHOT.jar"

echo [3/3] Starting React Frontend on http://localhost:5173 ...
start "RecallRadar Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo ========================================================
echo RecallRadar is launching!
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo ========================================================
