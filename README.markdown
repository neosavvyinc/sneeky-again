## Phantom Services

To create a new feature - use a branch:

`git checkout -b feature-block-by-convo`

Then push it to the remote

`git push origin feature-block-by-convo:feature-block-by-convo`

## Run your tests

`sbt test`

All of them should pass

## Start the server with assembly mode

`sbt clean assembly`
`./startServer.sh`

## Start the server with Revolver

`sbt`
`re-start`
