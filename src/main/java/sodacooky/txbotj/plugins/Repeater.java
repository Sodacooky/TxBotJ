package sodacooky.txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageSender;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.BadWordsChecker;

import java.util.Random;

/**
 * 进行群消息复读和私聊复读。
 * 群消息按概率复读并检查是否有敏感词，私聊消息全部复读也检查敏感词。
 */
@Component
public class Repeater implements IPlugin {

    @Autowired
    private BadWordsChecker badWordsChecker; //敏感词判断工具
    @Autowired
    private MessageSender messageSender;

    @Override
    public int getPriority() {
        return -100;
    }

    @Override
    public String getName() {
        return "Repeater";
    }

    @Override
    public boolean onPrivateMessage(JsonNode message) {
        //获取消息信息
        String msgContent = message.get("message").asText();
        long msgUser = message.get("user_id").asLong();
        //检查敏感词
        if (badWordsChecker.isContainsBadWords(msgContent)) {
            return false;//有敏感词也没有继续传递下去的必要了
        }
        //送回
        messageSender.sendPrivateMessage(msgUser, msgContent);
        return true;//就算复读了，也可能有别的插件需要工作
    }

    @Override
    public boolean onGroupMessage(JsonNode message) {
        //获取消息信息
        String msgContent = message.get("message").asText();
        long msgGroup = message.get("group_id").asLong();
        //抽签
        if (new Random().nextInt(100) != 0) {
            return true;//没抽中
        }
        //检查敏感词
        if (badWordsChecker.isContainsBadWords(msgContent)) {
            return false;//有敏感词也没有继续传递下去的必要了
        }
        //复读
        messageSender.sendGroupMessage(msgGroup, msgContent);
        return true;//就算复读了，也可能有别的插件需要工作
    }
}
