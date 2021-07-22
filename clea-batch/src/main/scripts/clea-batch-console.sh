#!/usr/bin/env bash

PROGNAM=$(basename $0)

die()  { echo "`date "+%Y-%m-%d %T.999"` ERROR 0 --- [     clea-batch] clea-batch.sh                            : $*" 1>&2 ; exit 1; }
info() { echo "`date "+%Y-%m-%d %T.999"`  INFO 0 --- [     clea-batch] clea-batch.sh                            : $*"; }

WORKDIR=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}
BUCKET=${BUCKET:-}

BUCKET_OUTSCALE=${BUCKET_OUTSCALE:-$BUCKET}
BUCKET_SCALEWAY=${BUCKET_SCALEWAY:-$BUCKET}

PROFILE_OUTSCALE=${PROFILE_OUTSCALE:-s3outscale} 
PROFILE_SCALEWAY=${PROFILE_SCALEWAY:-s3scaleway}

ENDPOINT_OUTSCALE=${ENDPOINT_OUTSCALE:-} # use https://oos.eu-west-2.outscale.com/ https://oos.cloudgouv-eu-west-1.outscale.com
ENDPOINT_SCALEWAY=${ENDPOINT_SCALEWAY:-} # https://s3.fr-par.scw.cloud



set -o pipefail  # trace ERR through pipes
set -o errtrace  # trace ERR through 'time command' and other functions
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
#set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set +e


[ -n "${BUCKET_OUTSCALE}" ] || die "Environment variable BUCKET_OUTSCALE required"

if ! java -jar clea-batch.jar $@ ; then
    die "Java batch fails"
fi


info "Copying files...."


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

# Copy clusterfiles to s3
# =======================


info "Copying to OUTSCALE ...."
AWS_OPTS="--profile=$PROFILE_OUTSCALE --endpoint-url=$ENDPOINT_OUTSCALE --no-progress"
BUCKET="$BUCKET_OUTSCALE"

# All files except indexCluster.json
aws $AWS_OPTS s3 sync --acl public-read --exclude=clusterIndex.json $WORKDIR s3://${BUCKET}/v1 || die "AWS s3 fails to copy cluster files to bucket"
# only indexCluster.json at the root of "v1"
aws $AWS_OPTS s3 cp   --acl public-read $(find $WORKDIR -type f -name clusterIndex.json) s3://${BUCKET}/v1/ || die "AWS s3 fails to copy clusterIndex file to bucket"

# COPY TO SCALEWAY (optional)
# --------------------
if [ -n "$BUCKET_SCALEWAY" ] &&  [ -n "$PROFILE_SCALEWAY" ]  &&  [ -n "$ENDPOINT_SCALEWAY" ] ; then
  info "Copying to SCALEWAY ...."
  AWS_OPTS="--profile=$PROFILE_SCALEWAY --endpoint-url=$ENDPOINT_SCALEWAY --no-progress"
  BUCKET=$BUCKET_SCALEWAY

  # All files except indexCluster.json
  aws $AWS_OPTS s3 sync --acl public-read --exclude=clusterIndex.json $WORKDIR s3://${BUCKET}/v1 || die "AWS s3 fails to copy cluster files to bucket"

  # only indexCluster.json at the root of "v1"
  aws $AWS_OPTS s3 cp --acl public-read $(find $WORKDIR -type f -name clusterIndex.json) s3://${BUCKET}/v1/ || die "AWS s3 fails to copy clusterIndex file to bucket"
fi

# purge batch temporary files
info "Purging working files"
rm -rf $WORKDIR
info "End of clea-batch"
