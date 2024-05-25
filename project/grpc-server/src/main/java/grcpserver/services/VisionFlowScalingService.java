package grcpserver.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.InstanceGroupManagersClient;
import com.google.cloud.compute.v1.ListManagedInstancesInstanceGroupManagersRequest;
import com.google.cloud.compute.v1.ManagedInstance;
import com.google.cloud.compute.v1.Operation;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class VisionFlowScalingService extends VisionFlowScalingServiceGrpc.VisionFlowScalingServiceImplBase {
    private final Logger logger = Logger.getLogger(VisionFlowScalingService.class.getName());
    private static final String PROJECT_ID = "cn2324-t1-g05";
    private static final String ZONE = "europe-west1-b";
    private static final List<String> instanceGroups = Arrays.asList(
            "instance-group-labels-app",
            "instance-group-grpc-server"
    );
    InstanceGroupManagersClient managersClient = InstanceGroupManagersClient.create();

    public VisionFlowScalingService() throws IOException {
    }

    @Override
    public void listManagedInstanceGroups(Empty request, StreamObserver<ManagedInstanceGroupResponse> responseObserver) {
        if (instanceGroups.isEmpty()) {
            logger.severe("Error listing instance groups, no instance groups found");
            responseObserver.onError(new IllegalStateException("No instance groups found"));
        }
        for (String instanceGroup : instanceGroups) {
            ManagedInstanceGroupResponse responseBuilder = ManagedInstanceGroupResponse
                    .newBuilder()
                    .setManagedInstanceGroup(instanceGroup)
                    .build();
            responseObserver.onNext(responseBuilder);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void resizeManagedInstanceGroup(
            ManagedInstanceGroupResizeRequest request, StreamObserver<Empty> responseObserver) {
        int newSize = request.getNewSize();
        String instanceGroupName = request.getManagedInstanceGroupName();
        logger.info("Resizing instance group " + instanceGroupName + " to " + newSize + " instances");
        try {
            OperationFuture<Operation, Operation> result = managersClient.resizeAsync(
                    PROJECT_ID,
                    ZONE,
                    instanceGroupName,
                    newSize
            );
            Operation oper = result.get();
            System.out.println("Resizing with status " + oper.getStatus());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.severe("Error resizing instance group: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void listManagedInstanceGroupVMs(ManagedInstanceNameRequest request, StreamObserver<ManagedInstanceGroupVMResponse> responseObserver) {

        String instanceGroupName = request.getManagedInstanceGroupName();
        if (!instanceGroups.contains(instanceGroupName)) {
            logger.severe("Error listing instance group VMs, instance group not found");
            responseObserver.onError(new IllegalStateException("Instance group not found"));
        }
        ListManagedInstancesInstanceGroupManagersRequest actualRequest =
                ListManagedInstancesInstanceGroupManagersRequest.newBuilder()
                        .setInstanceGroupManager(instanceGroupName)
                        .setProject(PROJECT_ID)
                        .setReturnPartialSuccess(true)
                        .setZone(ZONE)
                        .build();

        for (ManagedInstance instance :
                managersClient.listManagedInstances(actualRequest).iterateAll()) {
            ManagedInstanceGroupVMResponse responseBuilder = ManagedInstanceGroupVMResponse.newBuilder()
                    .setName(instance.getInstance())
                    .setStatus(instance.getInstanceStatus())
                    .build();
            responseObserver.onNext(responseBuilder);
        }
        responseObserver.onCompleted();
    }
}
