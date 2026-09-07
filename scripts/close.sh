#!/bin/bash
# ---------------------------------
# close.sh
# 用于关闭指定模块
# 参数1：模块名
# ---------------------------------

MODULE=$1
echo "[close.sh] Closing module: $MODULE"
sleep 2

PID=$(pgrep -f "$MODULE")
if [ -n "$PID" ]; then
    echo "[close.sh] Killing PID $PID"
    kill -9 "$PID"
else
    echo "[close.sh] No process found for $MODULE"
fi

echo "[close.sh] Done"
