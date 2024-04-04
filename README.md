# Template

[Directory](template) with the project template which includes:

- `grpcContract` module: contains the contract definition in a proto file;
- `grpcServer` module: contains the server implementation;
- `grpcClient` module: contains the client implementation.

## Generate Contract Jar

In the `grpcContract` module, access the Maven Task panel to generate the jar file.

1. Acess `Lifecycle` task aggregator and run the `package` task;
2. Ensure the generated jar file were placed in the `grpcContract/target` directory;
3. Install the generated jar file in the local maven repository by running the `install` task
