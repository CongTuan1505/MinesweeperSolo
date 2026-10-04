#!/bin/sh
# Chạy Client. Tham số tùy chọn: địa chỉ IP của máy chạy Server (mặc định localhost)
# Ví dụ: ./run-client.sh                 (kết nối localhost, dùng khi Server chạy cùng máy)
#        ./run-client.sh 192.168.1.5     (kết nối sang máy khác trong cùng mạng LAN)
cd "$(dirname "$0")"
if [ -z "$1" ]; then
  mvn -q exec:java -Dexec.mainClass=domin.Client
else
  mvn -q exec:java -Dexec.mainClass=domin.Client -Dexec.args="$1"
fi
