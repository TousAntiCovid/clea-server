#!/usr/bin/env bats
#
# this script is suppose to be execute, by bats (https://github.com/bats-core/bats-core),
# from the root level of "clea" project.
#
# those tests check the batch itself, not the functionnality of the java batch,
# nor the command s3cmd. java and s3cmd can return 0 (success) or <>0 (fails) 
# 
# Example of execution from "clea" root folder
#   $ docker run -it --rm -v $PWD:/app --workdir=/app bats/bats  src/test/bats
# ==============================================================================
load '../../../target/bats/support/load'
load '../../../target/bats/assert/load'

setup() {
    export BUCKET=clea-batch
    echo "Setting BUCKET=clea-batch"
}

@test "Should fail if no BUCKET env" {
    unset BUCKET
    run src/main/scripts/clea-batch-console.sh

    assert_failure
    assert_output --partial "BUCKET_OUTSCALE required"
}

@test "Should fail on Java failure" {
    java() { echo "CALLING_JAVA ${*}" ; return 1; }
    export -f java

    run src/main/scripts/clea-batch-console.sh

    assert_failure
    assert_output --partial "Java batch fails"
}

@test "Should fail if not exists output dir" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    run src/main/scripts/clea-batch-console.sh

    assert_failure
    assert_output --regexp "Working directory .+ not exists"
}

@test "Should fail if not enough generated files" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*

    run src/main/scripts/clea-batch-console.sh

    assert_failure
    assert_output --partial "not enough clusterfiles to continue"
}

@test "Should fail if no Cluster index" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*
    touch    /tmp/v1/123/ab.json
    touch    /tmp/v1/123/ac.json
    touch    /tmp/v1/123/ad.json
    
    run src/main/scripts/clea-batch-console.sh

    assert_failure
    assert_output --partial "No clusterIndex.json generated"
}

@test "Should fail if S3 copy of cluster files returns error" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    # fails for s3cmd sync but not for s3cmd put
    aws() { echo "CALLING_AWS_S3 ${*}" ; [ $5 == "sync" ] && return 1 || return 0; }
    export -f aws

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*
    touch    /tmp/v1/123/ab.json
    touch    /tmp/v1/123/ac.json
    touch    /tmp/v1/123/ad.json
    touch    /tmp/v1/clusterIndex.json
    
    run src/main/scripts/clea-batch-console.sh
    assert_failure
    assert_output --partial "fails to copy cluster files"

}


@test "Should fail if S3 copy of cluster index returns error" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    # fails for s3cmd put but not for s3cmd sync
    aws() { echo "CALLING_AWS_S3 ${*}" ; [ $5 == "cp" ] && return 1 || return 0; }
    export -f aws

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*
    touch    /tmp/v1/123/ab.json
    touch    /tmp/v1/123/ac.json
    touch    /tmp/v1/123/ad.json
    touch    /tmp/v1/clusterIndex.json
    
    run src/main/scripts/clea-batch-console.sh
    assert_failure
    assert_output --partial "fails to copy clusterIndex"

}

@test "Should succeds with presence of ClusterIndex" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    aws() { echo "CALLING_AWS_S3 ${*}" ; return 0; }
    export -f aws

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*
    touch    /tmp/v1/123/ab.json
    touch    /tmp/v1/123/ac.json
    touch    /tmp/v1/123/ad.json
    touch    /tmp/v1/clusterIndex.json
    
    run src/main/scripts/clea-batch-console.sh

    assert_success
    assert_output --partial "CALLING_JAVA"
    assert_output --partial "s3 sync"
    assert_output --partial "s3 cp"
}

@test "Succeeds to purge temporary files at end of batch" {
    java() { echo "CALLING_JAVA ${*}" ; return 0; }
    export -f java

    aws() { echo "CALLING_AWS_S3 ${*}" ; return 0; }
    export -f aws

    mkdir -p /tmp/v1/123
    rm -rf /tmp/v1/123/*
    touch    /tmp/v1/123/ab.json
    touch    /tmp/v1/123/ac.json
    touch    /tmp/v1/123/ad.json
    touch    /tmp/v1/clusterIndex.json
    
    run src/main/scripts/clea-batch-console.sh

    assert_success

    # test purge
    nb_files=$(find /tmp/v1 -type f -print |wc -l)
    assert_equal "$nb_files" '0'    
}
