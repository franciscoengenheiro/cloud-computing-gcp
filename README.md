# Cloud Computing - gRPC Java

## Table of Contents

- [Template](#template)
- [Define a contract](#define-a-contract)
- [Generate Contract Jar](#generate-contract-jar)
- [Server Implementation](#server-implementation)
- [Client Implementation](#client-implementation)

## Template

The template [directory](template) contains a simple project structure to implement a gRPC service with all the
necessary modules:

- `grpcContract`: contains the contract definition in a proto file;
- `grpcServer`: contains the server implementation;
- `grpcClient`: contains the client implementation.

## Define a contract

In the `grpcContract` module a file with the `.proto` extension is used to define the service contract.
The contract defines the service operations, request and response messages, and the data types used in the service.
Example of a service contract:

```proto
// Service contract operations 
service Service {
  // ping server for testing service availability
  rpc isAlive(ProtoVoid) returns (TextMessage);
  // get first N even numbers 2,...,K
  rpc getEvenNumbers(IntNumber) returns (stream IntNumber);
  // add a sequence of numbers, ex: 1,2,10,5 = 18
  rpc addSeqOfNumbers (stream IntNumber) returns (IntNumber);
  // multiple add operations using a bidirectional stream
  rpc multipleAdd(stream AddOperands) returns (stream AddResult);
}
// data types ...
```

Which maps to the 4 possible operation types between a client and a server:

- Unary: `isAlive` operation;
- Server streaming: `getEvenNumbers` operation;
- Client streaming: `addSeqOfNumbers` operation;
- Bidirectional streaming: `multipleAdd` operation.

## Generate Contract Jar

In the `grpcContract` module, access the Maven Task panel to generate the jar file.

1. Acess `Lifecycle` task aggregator and run the `package` task;
2. Ensure the generated jar files were placed in the `grpcContract/target` directory;
3. Install the jar file in the local maven repository by running the `install` task

## Server Implementation

Given the service contract defined [previously](#define-a-contract), the server implementation should implement it in a
class that extends the generated abstract class by the gRPC framework. If the
service contract is named `Service`, the generated abstract class will be `ServiceGrpc.ServiceImplBase`.
Example:

```java
public class Service extends ServiceGrpc.ServiceImplBase {
    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        // implementation logic
    }

    @Override
    public void getEvenNumbers(IntNumber request, StreamObserver<IntNumber> responseObserver) {
        // implementation logic
    }

    @Override
    public StreamObserver<IntNumber> addSeqOfNumbers(StreamObserver<IntNumber> responseObserver) {
        // implementation logic
    }

    @Override
    public StreamObserver<AddOperands> multipleAdd(StreamObserver<AddResult> responseObserver) {
        // implementation logic
    }
}
```

Additional notes:

1. The input object `request` is the message sent by the client to the server, only available in methods where the
   client sends a single message to the server.
   The message object is generated by the gRPC framework based on the proto file definition.
   Since the corresponding data type is
   generated, the [builder](https://en.wikipedia.org/wiki/Builder_pattern) pattern can be used to create the message
   object. Example:

    ```java
    IntNumber request = IntNumber.newBuilder().setValue(10).build();
    ```

2. The input type `responseObserver` is used
   to send the response back to the client in a special type of `callback`, the client provided upon
   calling the service method.
   The `StreamObserver` interface is used to send responses back to the client,
   and follows the [observer](https://en.wikipedia.org/wiki/Observer_pattern) behavioral design pattern
   where `V` is the type of the response message:
    ```java
    public interface StreamObserver<V> {
        void onNext(V value); // signal a response message
        void onError(Throwable t); // signal an error in the response stream
        void onCompleted(); // signal the end of the response stream
    }
    ```

   > [!NOTE]
   > Even in the first operation type,
   > the unary operation (i.e.,
   > where the client sends a single message to the server and the server sends a single message back to the client),
   > the `StreamObserver` is still used to send the response back to the client.

3. A method that uses a `StreamObserver` object should call:
    - `onNext`: at least once to send a response to the client;
    - `onCompleted`: to signal the end of the response stream.

4. If a method returns a `StreamObserver` object, then an instance of a class that implements the `StreamObserver`
   interface should be returned. Anonymously implementing the interface is a common practice:

    ```java
    @Override
    public StreamObserver<IntNumber> addSeqOfNumbers(StreamObserver<IntNumber> responseObserver) {
        return new StreamObserver<IntNumber>() {
            int sum = 0;
            @Override
            public void onNext(IntNumber value) {
                sum += value.getValue();
            }

            @Override
            public void onError(Throwable t) {
                // handle error
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(IntNumber.newBuilder().setValue(sum).build());
                responseObserver.onCompleted();
            }
        };
    }
    ```

   > [!NOTE]
   > The `responseObserver` methods are called inside the `StreamObserver` implementation in methods that return a
   > `StreamObserver` object.
   > This is done because the `StreamObserver` object returned by the method is the one that will be used by the client
   to
   > receive the responses whereas the `responseObserver` input object is the one that will be used by the server to
   send
   > the
   > responses back to the client.

## Client Implementation

Given the service contract defined [previously](#define-a-contract),
the client implementation should define methods to call the service operations.

### Channel

A channel is used to connect to the server. The gRPC framework provides a `ManagedChannel` class to create a channel.

```java
private static String svcIP = "localhost";
private static int svcPort = 8000;
private static ManagedChannel channel = ManagedChannelBuilder
        .forAddress(svcIP, svcPort)
        // Channels are secure by default (via SSL/TLS).
        // For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build();
```

### Stubs

A stub is a client-side representation of the server. The gRPC framework generates two types of stubs:

1. `Blocking stub`: the client sends a request to the server and waits for the response.
    ```java
    private static ServiceGrpc.ServiceBlockingStub blockingStub = ServiceGrpc.newBlockingStub(channel);
    static void isAliveCall() {
        TextMessage reply = blockingStub.isAlive(ProtoVoid.newBuilder().build());
        // additional logic...
    }
    ```
2. `Non-blocking stub`: the client sends a request to the server and continues to execute other tasks while waiting for
   the response.
    ```java
    private static ServiceGrpc.ServiceStub noBlockStub = ServiceGrpc.newStub(channel);
    static void addSequenceOfNumbersCall() {
        StreamObserver<IntNumber> streamNumbers = noBlockStub.addSeqOfNumbers(new StreamObserver<IntNumber>() {
            @Override
            public void onNext(IntNumber intNumber) {
                // handle response
            }

            @Override
            public void onError(Throwable throwable) {
                // handle error
            }

            @Override
            public void onCompleted() {
                // handle completion
            }
        });
        // at least one onNext should be called
        streamNumbers.onNext(IntNumber.newBuilder().setIntnumber(i).build());
        // additional logic...
        // signal the end of the stream
        streamNumbers.onCompleted();
        // Note that client has sent all requests, but needs synchronization
        // to terminate after get the final result
   }
   ```

| Operation type          | Blocking Stub | Non-blocking Stub |
|-------------------------|---------------|-------------------|
| Unary                   | ✅             | ✅                 |
| Server streaming        | ✅             | ✅                 |
| Client streaming        | ❌             | ✅                 |
| Bidirectional streaming | ❌             | ✅                 |
