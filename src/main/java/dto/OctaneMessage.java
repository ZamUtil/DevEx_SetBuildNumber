package dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneMessage {

    @JsonProperty("server_url")
    private String serverUrl;
    @JsonProperty("event_type")
    private String eventType;
    @JsonProperty("sharedspace_id")
    private long spaceId;
    @JsonProperty("workspace_id")
    private long workspaceId;
    private List<Data> data;

    public OctaneMessage() {
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(long spaceId) {
        this.spaceId = spaceId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }


    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }


    @Override
    public String toString() {
        return "dto.OctaneMessage{" +
                "server_url='" + serverUrl + '\'' +
                ", event_type='" + eventType + '\'' +
                ", sharedspace_id=" + spaceId +
                ", workspace_id=" + workspaceId +
                ", data=" + data +
                '}';
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Data {
        private Entity entity;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> changes;

        public Data() {
        }

        public Entity getEntity() {
            return entity;
        }

        public void setEntity(Entity entity) {
            this.entity = entity;
        }

        public Map<String, Object> getChanges() {
            return changes;
        }

        public void setChanges(Map<String, Object> changes) {
            this.changes = changes;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "entity=" + entity +
                    ", changes=" + changes +
                    '}';
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Entity {
        private String type;
        private String id;
        private JsonNode commits;

        public Entity() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public JsonNode getCommits() {
            return commits;
        }

        public void setCommits(JsonNode commits) {
            this.commits = commits;
        }
    }

}

