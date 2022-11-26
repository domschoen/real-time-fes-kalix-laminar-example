# Backend part: using Kalix

## Building

You can use [sbt](https://www.scala-sbt.org/) to build your project,
which will also take care of generating code based on the `.proto` definitions:

```
sbt compile
```

## Running Locally

1. Make sure the Docker deamon is running on your machine

2. Run the proxy with 
   ```
   docker-compose up
   ```
   > On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007),
   > or for a `USER_FUNCTION_HOST` environment variable to be set manually.
3. In another terminal window, run the application:

   ```
   sbt run
   ```
With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. For example, using `curl`:

```shell
> curl -XPOST -H "Content-Type: application/json" localhost:9000/project/1/invest -d '{
    "amount" : 100.0,
    "user" : "domschoen"
}'
```

For more information on Kalix, look at https://docs.kalix.io/index.html


