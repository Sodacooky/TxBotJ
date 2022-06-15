package txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.api.ApiExecutor;
import txbotj.plugins.utils.badwordchecker.BadWordsChecker;

import java.util.Random;

public class GroupRepeater extends Plugin {

    public GroupRepeater() {
        super("GroupRepeater");
    }

    @Override
    public boolean onGroupMessage(JsonNode message) {
        if (new Random().nextInt(1000) < 10) {
            //命中，试图复读
            String msg = message.get("message").asText();
            //判断是否可以复读
            if (!isContentAvailable(msg)) return false;
            //send back
            ApiExecutor.sendGroupMessage(message.get("group_id").asLong(), msg);
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

    //判断内容是否能安全地复读
    private boolean isContentAvailable(String message) {
        //过长
        if (message.length() > 256) return false;//约128个中文汉字
        //过短
        if (message.length() < 8) return false;//约4个中文汉字
        //带链接
        if (message.contains("http")) return false;
        //图片
        if (message.contains("[CQ:image")) return false;
        //敏感词过滤
        if (BadWordsChecker.isContainsBadWords(message)) return false;
        //success
        return true;
    }
}
