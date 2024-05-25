package grcpserver;

import com.google.cloud.ServiceOptions;
import grcpserver.services.VisionFlowFunctionalService;
import grcpserver.services.VisionFlowScalingService;
import io.grpc.ServerBuilder;

import java.util.Objects;
import java.util.logging.Logger;

public class GrpcServer {
    private static int svcPort = 8000;
    private static final Logger logger = Logger.getLogger(GrpcServer.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            String projectId = ServiceOptions.getDefaultProjectId();
            Objects.requireNonNull(projectId, "GOOGLE_APPLICATION_CREDENTIALS environment variable not set");
            logger.info("Connected to project: " + projectId);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    // Add one or more services.
                    // The Server can host many services in same TCP/IP port
                    .addService(new VisionFlowFunctionalService(projectId))
                    .addService(new VisionFlowScalingService(projectId))
                    .build();
            svc.start();
            logger.info("Server started, listening on " + svcPort);
            // Java virtual machine shutdown hook
            // to capture normal or abnormal exits
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));
            // Waits for the server to become terminated
            svc.awaitTermination();
        } catch (Exception ex) {
            logger.severe("Server failed to start: " + ex.getMessage());
        }
    }

}
