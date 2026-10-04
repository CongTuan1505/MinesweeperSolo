@echo off
REM Chạy Client. Tham số tùy chọn: địa chỉ IP máy chạy Server (mặc định localhost)
REM Vi du: run-client.bat                  (ket noi localhost)
REM        run-client.bat 192.168.1.5      (ket noi sang may khac trong LAN)
cd /d %~dp0
if "%~1"=="" (
  call "%~dp0mvnw.cmd" -q exec:java -Dexec.mainClass=domin.Client
) else (
  call "%~dp0mvnw.cmd" -q exec:java -Dexec.mainClass=domin.Client -Dexec.args=%1
)
