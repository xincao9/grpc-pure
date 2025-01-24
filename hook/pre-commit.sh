#!/bin/bash

ROOT_DIR=$(git rev-parse --show-toplevel)
cd $ROOT_DIR

mvn formatter:format
if [ $? -ne 0 ]; then
        echo "Maven 构建格式化，提交中止。"
        exit 1
fi

mvn clean install
if [ $? -ne 0 ]; then
        echo "Maven 构建失败，提交中止。"
        exit 1
fi
