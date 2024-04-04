package grpcclientapp;

import io.grpc.stub.StreamObserver;
import servicestubs.IntNumber;

public class PrimeNumbersStream implements StreamObserver<IntNumber> {
    // StreamObserver for PrimeNumbers
    boolean completed=false;
    @Override
    public void onNext(IntNumber reply) {
        System.out.println("More one prime number:"+reply.getIntnumber());
    }
    @Override
    public void onError(Throwable throwable) {
        System.out.println("Completed with error:"+throwable.getMessage());
        completed=true;
    }
    @Override
    public void onCompleted() {
        System.out.println("Prime numbers completed");
        completed=true;
    }

    public boolean isCompleted() {
        return completed;
    }
}
