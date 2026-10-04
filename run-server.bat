@echo off
REM Chạy Server. Tham số tùy chọn: số cổng (mặc định 5555)
REM Vi du: run-server.bat          (cong 5555)
REM        run-server.bat 6000     (doi sang cong 6000)
cd /d %~dp0
if "%~1"=="" (
  call "%~dp0mvnw.cmd" -q exec:java -Dexec.mainClass=domin.Server
) else (
  call "%~dp0mvnw.cmd" -q exec:java -Dexec.mainClass=domin.Server -Dexec.args=%1
)
