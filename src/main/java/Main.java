import com.microfocus.octane.websocket.OctaneWSClientContext;
import com.microfocus.octane.websocket.OctaneWSClientService;
import com.microfocus.octane.websocket.OctaneWSEndpointClient;
import handler.UpdateFixedInBuildMessageHandler;
import org.eclipse.jetty.http.HttpMethod;
import util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Properties;

import static java.net.CookiePolicy.ACCEPT_ALL;

public class Main {

    public static String octaneUrl;
    public static String clientId;
    public static String clientSecret;
    public static String jobName;
    public static String fixedInBuildField;

    private static HttpClient httpClient;

    public static void main(String[] args) {
        readProperties();

        OctaneWSClientContext context = OctaneWSClientContext.builder()
                .setEndpointUrl("wss://" + octaneUrl + ":443/messaging/shared_spaces/1001/webhooks")
                .setClient(clientId)
                .setSecret(clientSecret)
                .setCustomHeaders(Map.of("ALM_OCTANE_TECH_PREVIEW", "true"))
                .build();

        initHttpClient();

        OctaneWSClientService.getInstance().initClient(new OctaneWSEndpointClient(context) {
            @Override
            public void onStringMessage(String message) {
                doLogin();
                new UpdateFixedInBuildMessageHandler(message, httpClient, jobName, fixedInBuildField).handle();
            }
        });

        System.out.println("Waiting for messages:");
    }

    private static void readProperties() {
        String resourceName = "config.properties";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
            properties.load(resourceStream);

            octaneUrl = properties.get("OCTANE_URL").toString();
            clientId = properties.get("CLIENT_ID").toString();
            clientSecret = properties.get("CLIENT_SECRET").toString();
            jobName = properties.get("JOB_NAME").toString();
            fixedInBuildField = properties.get("FIXED_IN_BUILD_FIELD").toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.cookieHandler(new CookieManager(null, ACCEPT_ALL));
        httpClient = builder.build();

        doLogin();
    }

    private static void doLogin() {
        String jsonPayload = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + clientSecret + "\"}";
        String url = "https://" + octaneUrl + ":443/authentication/sign_in";
        HttpRequest request = Utils.buildHttpRequest(HttpMethod.POST, url, jsonPayload);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Error while login, reason:" + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
