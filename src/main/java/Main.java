import com.microfocus.octane.websocket.OctaneWSClientContext;
import com.microfocus.octane.websocket.OctaneWSClientService;
import com.microfocus.octane.websocket.OctaneWSEndpointClient;
import handler.UpdateFixedInBuildMessageHandler;
import org.eclipse.jetty.http.HttpMethod;
import util.Utils;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static java.net.CookiePolicy.ACCEPT_ALL;

public class Main {

/*    public static final String OCTANE_URL = "yuri-2.almoctane.com";
    public static final String CLIENT_ID = "devex_jeg540y9vzo87tjggp0o9mqdw";
    public static final String CLIENT_SECRET = ")229119633672230179134V";*/
    public static final String OCTANE_URL = "qa91.almoctane.com";
    public static final String CLIENT_ID = "devEx_pnx3ly3qo9xx6hvengy5906vg";
    public static final String CLIENT_SECRET = "+90171961022472216547E";

    private static HttpClient httpClient;

    public static void main(String[] args) {
        OctaneWSClientContext context = OctaneWSClientContext.builder()
                .setEndpointUrl("wss://" + OCTANE_URL + ":443/messaging/shared_spaces/1001/webhooks")
                .setClient(CLIENT_ID)
                .setSecret(CLIENT_SECRET)
                .setCustomHeaders(Map.of("ALM_OCTANE_TECH_PREVIEW", "true"))
                .build();

        initHttpClient();

        OctaneWSClientService.getInstance().initClient(new OctaneWSEndpointClient(context) {
            @Override
            public void onStringMessage(String message) {
                doLogin();
                new UpdateFixedInBuildMessageHandler(message, httpClient).handle();
            }
        });

        System.out.println("Waiting for messages:");
    }

    private static void initHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.cookieHandler(new CookieManager(null, ACCEPT_ALL));
        httpClient = builder.build();

        doLogin();
    }

    private static void doLogin() {
        String jsonPayload = "{\"client_id\":\"" + CLIENT_ID + "\",\"client_secret\":\"" + CLIENT_SECRET + "\"}";
        String url = "https://" + OCTANE_URL + ":443/authentication/sign_in";
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
