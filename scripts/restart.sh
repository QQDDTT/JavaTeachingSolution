#!/bin/bash
# ---------------------------------
# restart.sh
# 用于重启指定模块
# 参数1：模块名
# ---------------------------------

MODULE=$1
echo "[restart.sh] Restarting module: $MODULE"
sleep 2

# 示例：杀掉旧进程并重启
PID=$(pgrep -f "$MODULE")
if [ -n "$PID" ]; then
    echo "[restart.sh] Killing PID $PID"
    kill -9 "$PID"
fi

# 重启
echo "[restart.sh] Starting new process..."
nohup java -jar "$MODULE.jar" >/dev/null 2>&1 &
echo "[restart.sh] Done"
