#!/bin/sh
# Chạy Server. Tham số tùy chọn: số cổng (mặc định 5555)
# Ví dụ: ./run-server.sh        (cổng 5555)
#        ./run-server.sh 6000  (đổi sang cổng 6000)
cd "$(dirname "$0")"
if [ -z "$1" ]; then
  mvn -q exec:java -Dexec.mainClass=domin.Server
else
  mvn -q exec:java -Dexec.mainClass=domin.Server -Dexec.args="$1"
fi
