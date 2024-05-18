package grcpserver;

import grcpserver.services.VisionFlowFunctionalService;
import io.grpc.ServerBuilder;

import java.util.logging.Logger;

public class GrpcServer {
    private static int svcPort = 8000;
    private static Logger logger = Logger.getLogger(GrpcServer.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    // Add one or more services.
                    // The Server can host many services in same TCP/IP port
                    .addService(new VisionFlowFunctionalService())
                    // TODO: add more services here
                    .build();
            svc.start();
            logger.info("Server started, listening on " + svcPort);
            // Java virtual machine shutdown hook
            // to capture normal or abnormal exits
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));
            // Waits for the server to become terminated
            svc.awaitTermination();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

}
