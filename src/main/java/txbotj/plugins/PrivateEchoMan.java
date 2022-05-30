package txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.api.ApiExecutor;

//私聊ping pong
public class PrivateEchoMan extends Plugin {

    public PrivateEchoMan() {
        super("PrivateEchoMan");
    }

    @Override
    public boolean onGroupMessage(JsonNode message) {
        //ignore
        return super.onGroupMessage(message);
    }

    @Override
    public boolean onPrivateMessage(JsonNode message) {
        //send back
        ApiExecutor.sendPrivateMessage(message.get("user_id").asLong(), message.get("message").asText());
        return super.onPrivateMessage(message);
    }

    @Override
    public boolean onOtherMessage(JsonNode message) {
        //ignore
        return super.onOtherMessage(message);
    }
}
