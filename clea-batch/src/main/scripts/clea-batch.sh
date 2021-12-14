#!/usr/bin/env bash

_die()  { echo "`date "+%Y-%m-%d %T.999"` ERROR 0 --- [     clea-batch] clea-batch.sh                            : $*" 1>&2 ; exit 1; }

# redirect logs to log file
CLEA_BATCH_LOG_FILE_PATH=dirname ${LOGGING_FILE_NAME:?missing logging file name}

mkdir -p "$CLEA_BATCH_LOG_FILE_PATH" || _die "can't create log dir: $CLEA_BATCH_LOG_FILE_PATH"
touch "$LOGGING_FILE_NAME" || _die "can't create log file: $LOGGING_FILE_NAME"

exec >> "$LOGGING_FILE_NAME"

source clea-batch-console.sh
