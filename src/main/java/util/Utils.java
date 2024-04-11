package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import dto.OctaneMessage;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpRequest;

public class Utils {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static HttpRequest buildHttpRequest(HttpMethod method, String url, String body) {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

        if (StringUtils.isNotEmpty(body)) {
            bodyPublisher = HttpRequest.BodyPublishers.ofString(body);
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(url).normalize())
                .header("Content-Type", "application/json")
                .method(method.toString(), bodyPublisher)
                .build();
    }
    public static HttpRequest buildHttpRequestWithExpand(HttpMethod method, String url, String body) {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

        if (StringUtils.isNotEmpty(body)) {
            bodyPublisher = HttpRequest.BodyPublishers.ofString(body);
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(url).normalize())
                .header("Content-Type", "application/json")
                .header("Hpeclienttype", "HPE_MQM_UI")
                .method(method.toString(), bodyPublisher)
                .build();
    }

    public static OctaneMessage parseMessage(String message) {
        try {
            return MAPPER.readValue(message, OctaneMessage.class);
        } catch (UnrecognizedPropertyException e) {
            System.out.println("found not relevant fields, ignoring message");
            return null;
        } catch (JsonProcessingException e) {
            System.out.println("Error while parsing message");
            e.printStackTrace();
            return null;
        }
    }

    public static String getBaseUrl(OctaneMessage octaneMessage) {
        return octaneMessage.getServerUrl() +
                "/api/shared_spaces/" +
                octaneMessage.getSpaceId() +
                "/workspaces/" +
                octaneMessage.getWorkspaceId();
    }
}
