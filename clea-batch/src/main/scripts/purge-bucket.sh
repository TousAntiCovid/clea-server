#!/usr/bin/env bash

info() { echo "`date "+%Y-%m-%d %T.999"`  INFO 0 --- [     purge-bucket] purge-bucket.sh                            : $*"; }

WORKDIR=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}
BUCKET=${BUCKET:-}

BUCKET_OUTSCALE=${BUCKET_OUTSCALE:-$BUCKET}
PROFILE_OUTSCALE=${PROFILE_OUTSCALE:-s3outscale} 
ENDPOINT_OUTSCALE=${ENDPOINT_OUTSCALE:-}

BUCKET_FILES_RETENTION_IN_DAYS=${BUCKET_FILES_RETENTION_IN_DAYS:-15}

TODAY_MINUS_RETENTION_DAYS=$(date --date="${BUCKET_FILES_RETENTION_IN_DAYS} days ago" +%Y-%m-%d)

info "Listing bucket files..."
BUCKET_FILES=${aws --profile=$PROFILE_OUTSCALE --endpoint-url=$ENDPOINT_OUTSCALE s3 ls --recursive s3://${BUCKET}/v1}

info "Filtering to keep files older than ${TODAY_MINUS_RETENTION_DAYS}..."
BUCKET_FILES_TO_REMOVE=${${BUCKET_FILES} | awk -v date=$TODAY_MINUS_RETENTION_DAYS '$1 < date {print $4}'}

info "Extract iterations to remove..."
BUCKET_ITERATIONS_TO_REMOVE=${BUCKET_FILES_TO_REMOVE | cut -f1 -d"/" | uniq -c}

info "Get first and last iteration to remove..."
START=${BUCKET_ITERATIONS_TO_REMOVE[0]}
END=${BUCKET_ITERATIONS_TO_REMOVE[0]}

for i in "${BUCKET_ITERATIONS_TO_REMOVE[@]}"
do
    if [[ "$i" -gt "$END" ]]; then
        END="$i"
    fi

    if [[ "$i" -lt "$START" ]]; then
        START="$i"
    fi
done

info "Purging bucket iterations..."
for ((i="$START"; i<="$END"; i++))
do
    aws s3 rm aws s3 --profile=$PROFILE_OUTSCALE --endpoint-url=$ENDPOINT_OUTSCALE rm s3://${BUCKET}/"$i" --recursive
done