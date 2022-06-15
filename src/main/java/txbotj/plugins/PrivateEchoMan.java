package txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.api.ApiExecutor;
import txbotj.plugins.utils.badwordchecker.BadWordsChecker;

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
        String content = message.get("message").asText();
        //check bad words
        if (BadWordsChecker.isContainsBadWords(content)) return false;
        //send back
        ApiExecutor.sendPrivateMessage(message.get("user_id").asLong(), content);
        return super.onPrivateMessage(message);
    }

    @Override
    public boolean onOtherMessage(JsonNode message) {
        //ignore
        return super.onOtherMessage(message);
    }
}
