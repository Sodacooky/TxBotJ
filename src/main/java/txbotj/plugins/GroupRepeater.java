package txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.api.ApiExecutor;

import java.util.Random;

public class GroupRepeater extends Plugin {

    public GroupRepeater() {
        super("GroupRepeater");
    }

    @Override
    public boolean onGroupMessage(JsonNode message) {
        if (new Random().nextInt(10) == 0) {
            //hit, send back
            ApiExecutor.sendGroupMessage(message.get("group_id").asLong(), message.get("message").asText());
        }
        return super.onGroupMessage(message);
    }

    @Override
    public boolean onPrivateMessage(JsonNode message) {
        return super.onPrivateMessage(message);
    }

    @Override
    public boolean onOtherMessage(JsonNode message) {
        return super.onOtherMessage(message);
    }
}
