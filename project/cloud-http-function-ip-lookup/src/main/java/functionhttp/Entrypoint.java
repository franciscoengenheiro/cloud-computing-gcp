package functionhttp;

import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.BufferedWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Entrypoint implements HttpFunction {

    private final Logger logger = Logger.getLogger(Entrypoint.class.getName());
    private static final String PROJECT_ID = "cn2324-t1-g04";
    private static final String ZONE = "europe-west3-c";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String instanceGroupName = request.getFirstQueryParameter("instance-group").orElseThrow();
        logger.info("Instances of instance group: " + instanceGroupName);
        BufferedWriter writer = response.getWriter();
        try (InstancesClient client = InstancesClient.create()) {
            List<String> ips = StreamSupport.stream(client.list(PROJECT_ID, ZONE).iterateAll().spliterator(), false)
                    .filter(instance -> "RUNNING".equals(instance.getStatus())
                            && instance.getName().contains(instanceGroupName))
                    .map(instance -> instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP())
                    .collect(Collectors.toList());

            logger.info("IP addresses: " + ips);

            // Join the shuffled IP addresses into a single string and write to the response
            String output = String.join(";", ips);
            logger.info("Output: " + output);
            writer.write(output);
        }
    }
}
