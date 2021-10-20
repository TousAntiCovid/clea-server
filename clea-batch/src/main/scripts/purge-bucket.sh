#!/usr/bin/env bash

PROFILE=
BUCKET=
ENDPOINT=

BUCKET_FILES_RETENTION_IN_DAYS=15

TODAY_MINUS_RETENTION_DAYS=$(date --date="${BUCKET_FILES_RETENTION_IN_DAYS} days ago" +%Y-%m-%d)

echo "Listing bucket files and extracting iterations older than $TODAY_MINUS_RETENTION_DAYS"
ITERATIONS_TO_REMOVE=$(
    aws --profile=$PROFILE --endpoint-url=$ENDPOINT s3 ls --recursive s3://${BUCKET}/v1 \
        | awk -v "date=$TODAY_MINUS_RETENTION_DAYS" '$1 < date {print $4}' \
        | grep '^v1/[0-9]*/' \
        | cut -d'/' -f 2 \
        | sort \
        | uniq
)

echo "Found $(echo -n "$ITERATIONS_TO_REMOVE" | wc -l) iterations to remove"

for i in $ITERATIONS_TO_REMOVE ; do
    echo "Removing iteration $i"
    aws s3 --profile=$PROFILE --endpoint-url=$ENDPOINT rm "s3://$BUCKET/v1/$i" --recursive
done
