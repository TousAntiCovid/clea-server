#!/usr/bin/env bash

_die()  { echo "`date "+%Y-%m-%d %T.999"` ERROR 0 --- [     clea-batch] clea-batch.sh                            : $*" 1>&2 ; exit 1; }

# redirect logs to log file
CLEA_BATCH_LOG_FILE_PATH=${CLEA_BATCH_LOG_FILE_PATH:-/logs}
CLEA_BATCH_LOG_FILE_NAME=${CLEA_BATCH_LOG_FILE_NAME:-clea-batch}
mkdir -p "$CLEA_BATCH_LOG_FILE_PATH" || _die "can't create log dir: $CLEA_BATCH_LOG_FILE_PATH"
touch "${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.log" || _die "can't create log file: ${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.log"
touch "${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.error.log" || _die "can't create log file: ${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.error.log"

exec >> "${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.log"
exec 2>> "${CLEA_BATCH_LOG_FILE_PATH}/${CLEA_BATCH_LOG_FILE_NAME}.error.log"

source clea-batch-console.sh
