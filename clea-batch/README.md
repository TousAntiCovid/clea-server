## Cléa Batch explainations

The batch component is to extract exposed visits recorded in database by the venue consumer component, computes 
the possible clusters that could have occurred, and outputs them as exploitable files for mobile phones to request.
The spring batch application is wrapped by a script, which, once the spring batch application has ended, takes the produced
files and uploads them to s3 buckets to make it available to mobile apps.

Hereafter is a schema describing the different steps of the application. The system uses various spring batch mechanics
to implement an ETL pipeline, from exposed visits data to cluster files. Please note that the script mentionned in last 
paragraph is not included in this schema perimeter.

![clea-batch schema](batch_schema.png)

Each rectangle represents a job step, and each arrow inside represents a step component. In and Out data format details
are provided to help read the code.