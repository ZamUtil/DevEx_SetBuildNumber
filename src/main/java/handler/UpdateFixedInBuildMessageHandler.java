package handler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dto.OctaneMessage;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import util.Utils;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import static util.Utils.getBaseUrl;
import static util.Utils.parseMessage;

public class UpdateFixedInBuildMessageHandler {

    public static final String JOB_NAME = "MQM-Root-POSTGRESQL-full-master";
    public static final String FIXED_IN_BUILD_FIELD = "fixed_in_build_udf";

    private final String message;
    private final HttpClient httpClient;

    public UpdateFixedInBuildMessageHandler(String message, HttpClient httpClient) {
        this.message = message;
        this.httpClient = httpClient;
    }

    public void handle() {
        System.out.println("Message is: " + message);
        OctaneMessage octaneMessage = parseMessage(message);
        if (octaneMessage == null || !octaneMessage.getEventType().equals("update")) {
            System.out.println("Message is not relevant");
            return;
        }

        Map<String, List<String>> relevantCommitsIdsPerEntity = getRelevantCommitsIdsPerEntity(octaneMessage);
        if (relevantCommitsIdsPerEntity.isEmpty()) {
            System.out.println("Commits to update not found.");
            return;
        }
        System.out.println("relevantCommitsIdsPerEntity:" + relevantCommitsIdsPerEntity);
        Map<String, Integer> versionsPerEntity = calculateLatestVersions(octaneMessage, relevantCommitsIdsPerEntity);
        updateVersions(octaneMessage, versionsPerEntity);
    }

    private Map<String, List<String>> getRelevantCommitsIdsPerEntity(OctaneMessage octaneMessage) {
        Map<String, List<String>> relevantCommitsIdsPerEntity = new HashMap<>();
        octaneMessage.getData()
                .stream()
                .filter(data -> {
                    if (!data.getChanges().containsKey("phase")) {
                        return false;
                    }

                    OctaneMessage.Entity entity = data.getEntity();
                    String type = entity.getType();
                    if (!Objects.equals(type, "defect") && !Objects.equals(type, "user_story")) {
                        return false;
                    }
                    return entity.getCommits().get("total_count").intValue() > 0;
                }).forEach(data -> {
                    OctaneMessage.Entity entity = data.getEntity();
                    ArrayNode commitsData = (ArrayNode) entity.getCommits().get("data");
                    List<String> commitsIds = new ArrayList<>();
                    commitsData.forEach(jsonNode -> commitsIds.add(jsonNode.get("id").textValue()));
                    relevantCommitsIdsPerEntity.put(entity.getId(), commitsIds);
                });

        return relevantCommitsIdsPerEntity;
    }

    //https://yuri-2.almoctane.com/api/shared_spaces/1001/workspaces/1002/scm_commits?fields=ci_build&query="(id IN 1001,1002,1003);ci_build={ci_job={name='Octane Full Master (PG)'}}"
    private Map<String, Integer> calculateLatestVersions(OctaneMessage octaneMessage, Map<String, List<String>> relevantCommitsIdsPerEntity) {
        try {
            String commitsIds = relevantCommitsIdsPerEntity.values().stream().flatMap(Collection::stream).collect(Collectors.joining(","));
            String url = getBaseUrl(octaneMessage) +
                    "/scm_commits?" +
                    "fields=" + URLEncoder.encode("ci_build{ci_job}", "UTF-8") +
                    "&query=" + URLEncoder.encode("\"id IN " + commitsIds + "\"", "UTF-8");

            HttpRequest request = Utils.buildHttpRequestWithExpand(HttpMethod.GET, url, null);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Integer> commitToVersionMap = buildCommitToVersionMap(response);

            return relevantCommitsIdsPerEntity.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                        List<String> commitIds = entry.getValue();
                        return commitIds.stream()
                                .map(commitToVersionMap::get)
                                .filter(Objects::nonNull)
                                .max(Comparator.comparingInt(o -> o))
                                .orElse(0);
                    }));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private Map<String, Integer> buildCommitToVersionMap(HttpResponse<String> response) {
        JSONObject jsonObject = new JSONObject(response.body());
        JSONArray commits = jsonObject.getJSONArray("data");

        Map<String, Integer> commitToVersion = new HashMap<>();
        for (int i = 0; i < commits.length(); i++) {
            JSONObject commit = commits.getJSONObject(i);
            String commitId = commit.get("id").toString();

            JSONArray builds = commit.getJSONObject("ci_build").getJSONArray("data");
            Integer version = null;
            for (int j = 0; j < builds.length(); j++) {
                JSONObject dataItem = builds.getJSONObject(j);
                String jobName = dataItem.getJSONObject("ci_job").getString("name");

                //filter out not relevant job
                if (JOB_NAME.equals(jobName)) {
                    try {
                        version = Integer.valueOf(dataItem.getString("name"));
                    } catch (Exception ignored) {
                    }
                }
            }

            if (version != null) {
                commitToVersion.put(commitId, version);
            }
        }

        return commitToVersion;
    }

    private void updateVersions(OctaneMessage octaneMessage, Map<String, Integer> versions) {
        String baseUrl = getBaseUrl(octaneMessage);
        try {
            versions.forEach((entityId, version) -> {
                System.out.println("Updating work_item: " + entityId + ", new version: " + version);
                String url = baseUrl + "/work_items/" + entityId;
                String body = "{\"" + FIXED_IN_BUILD_FIELD + "\": \"" + version + "\",\"id\":\"" + entityId + "\"}";
                HttpRequest request = Utils.buildHttpRequest(HttpMethod.PUT, url, body);
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
