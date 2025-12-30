package  com.springwater.easybot.bridge.model;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // 对应 C# 中只比较 PluginId
public class PluginManifest {

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("plugin_id")
    @EqualsAndHashCode.Include // 仅包含此字段用于 HashCode 和 Equals
    private String pluginId;

    @SerializedName("author")
    private String author;

    @SerializedName("description")
    private String description;

    @SerializedName("entry")
    private String entry;

    @SerializedName("icon")
    private String icon;

    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();

    @SerializedName("contents")
    private String contents = "";

    @SerializedName("links")
    private List<PluginLink> links = new ArrayList<>();

    @SerializedName("dependencies")
    private ClearScriptDependencies dependencies = new ClearScriptDependencies();
    
    @Data
    @NoArgsConstructor
    public static class ClearScriptDependencies {
        @SerializedName("load_before")
        private List<String> loadBefore = new ArrayList<>();

        @SerializedName("load_after")
        private List<String> loadAfter = new ArrayList<>();

        @SerializedName("requires")
        private List<String> requires = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class PluginLink {
        @SerializedName("name")
        private String name;

        @SerializedName("url")
        private String url;
    }
}