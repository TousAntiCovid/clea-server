#!/usr/bin/env bash

PROGNAM=$(basename $0)

die()  { echo "`date "+%Y-%m-%d %T.999"` ERROR 0 --- [     clea-batch] clea-batch.sh                            : $*" 1>&2 ; exit 1; }
info() { echo "`date "+%Y-%m-%d %T.999"`  INFO 0 --- [     clea-batch] clea-batch.sh                            : $*"; }

WORKDIR=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}

[ -n "${BUCKET_OUTSCALE}" ] || die "Environment variable BUCKET_OUTSCALE required"

PROFILE_OUTSCALE=${PROFILE_OUTSCALE:-s3outscale}

[ -n "${ENDPOINT_OUTSCALE}" ] || die "Environment variable ENDPOINT_OUTSCALE required"

BUCKET_FILES_RETENTION_IN_DAYS=${BUCKET_FILES_RETENTION_IN_DAYS:-15}

set -o pipefail  # trace ERR through pipes
set -o errtrace  # trace ERR through 'time command' and other functions
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
#set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set +e

copy_files_to_bucket() {

  local PROFILE=$1
  local BUCKET=$2
  local ENDPOINT=$3

  info "Copying to $ENDPOINT ...."
  AWS_OPTS="--profile=$PROFILE --endpoint-url=$ENDPOINT --no-progress"

  # All files except indexCluster.json
  aws $AWS_OPTS s3 sync --acl public-read --exclude=clusterIndex.json $WORKDIR s3://${BUCKET}/v1 || die "AWS s3 fails to copy cluster files to bucket"
  # only indexCluster.json at the root of "v1"
  aws $AWS_OPTS s3 cp   --acl public-read $(find $WORKDIR -type f -name clusterIndex.json) s3://${BUCKET}/v1/ || die "AWS s3 fails to copy clusterIndex file to bucket"

}

purge_old_bucket_iterations() {
  # purge bucket files older than x days
  # add --dryrun for testing purpose

  local PROFILE=$1
  local BUCKET=$2
  local ENDPOINT=$3

  TODAY_MINUS_RETENTION_DAYS=$(date --date="${BUCKET_FILES_RETENTION_IN_DAYS} days ago" +%Y-%m-%d)
  info "Purging bucket files older than ${TODAY_MINUS_RETENTION_DAYS}"
  aws --profile=$PROFILE --endpoint-url=$ENDPOINT s3 ls --recursive s3://${BUCKET}/v1 \
      | awk -v date=$TODAY_MINUS_RETENTION_DAYS '$1 < date {print $4}' \
      | xargs -n1 -t -I {} aws s3 --profile=$PROFILE --endpoint-url=$ENDPOINT rm s3://${BUCKET}/{}

}

if ! java -jar clea-batch.jar $@ ; then
    die "Java batch fails"
fi

# Test that output folder exists, computing NBFILES fails if folder doesn't exist
[ -d $WORKDIR ] || die "Working directory $WORKDIR not exists" 

# count that there is at least "n" cluster files (to not push empty list)
MIN_FILES=1
NBFILES=$(find $WORKDIR -type f | wc -l)
if [ $NBFILES  -lt $MIN_FILES ] ;then
    die "not enough clusterfiles to continue ($NBFILES  -lt $MIN_FILES)"
fi
NB_INDEX=$(find $WORKDIR -type f -name "clusterIndex.json" |wc -l)
if [ $NB_INDEX -eq 0 ] ; then
  die "No clusterIndex.json generated"
fi
if [ $NB_INDEX -gt 1 ] ; then
  die "Many clusterIndex.json found ($NB_INDEX), possible partial or failed batch already present"
fi

copy_files_to_bucket $PROFILE_OUTSCALE $BUCKET_OUTSCALE  $ENDPOINT_OUTSCALE

purge_old_bucket_iterations $PROFILE_OUTSCALE $BUCKET_OUTSCALE $ENDPOINT_OUTSCALE

# purge batch temporary files
info "Purging working files"
rm -rf $WORKDIR

info "End of clea-batch"


