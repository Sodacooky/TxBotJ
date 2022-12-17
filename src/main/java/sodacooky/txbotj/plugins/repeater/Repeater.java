package sodacooky.txbotj.plugins.repeater;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.badwords.BadWordsChecker;

import java.util.Calendar;
import java.util.Random;

/**
 * 进行群消息复读和私聊复读。
 * 群消息按概率复读并检查是否有敏感词，私聊消息全部复读也检查敏感词。
 */
@Slf4j
@Component
public class Repeater implements IPlugin {

    @Resource
    private BadWordsChecker badWordsChecker; //敏感词判断工具
    @Resource
    private MessageApi messageApi;
    @Resource
    private MyRepeaterMapper myRepeaterMapper; //复读机所用数据库操作

    @Override
    public int getPriority() {
        return -100;
    }

    @Override
    public String getName() {
        return "Repeater";
    }

    /**
     * 私聊复读
     *
     * @param cqMessageBody 消息json
     * @return 如果包含敏感词等，返回false
     */
    @Override
    public boolean onPrivateMessage(JsonNode cqMessageBody) {
        //检查
        if (isContentInappropriate(cqMessageBody.get("message").asText())) {
            //这种消息也没必要继续处理
            return false;
        }
        //送回
        messageApi.sendBackMessage(cqMessageBody, cqMessageBody.get("message").asText(), new Random().nextInt(2) + 1);
        //日志
        log.info("复读{}私聊消息: {}", cqMessageBody.get("user_id").asLong(), trimString(cqMessageBody.get("message").asText()));
        //就算复读了，也可能有别的插件需要工作
        return true;
    }

    /**
     * 群消息复读，
     * 收到消息时会更新数据库
     *
     * @param cqMessageBody 消息json
     * @return 如果包含敏感词等，返回false
     */
    @Override
    public boolean onGroupMessage(JsonNode cqMessageBody) {
        //获取消息信息
        String currentMessageContent = cqMessageBody.get("message").asText();
        long groupId = cqMessageBody.get("group_id").asLong();

        //检查数据库
        if (myRepeaterMapper.isExist(groupId) == 0) {
            //不存在，新建
            myRepeaterMapper.createNew(groupId);
        }

        //在更新数据库前获取上一条消息
        String previousMessageContent = myRepeaterMapper.getLastMessageContent(groupId);
        //更新数据库，上一条消息
        myRepeaterMapper.updateLastMessageContent(groupId, currentMessageContent);

        //检查
        if (isContentInappropriate(currentMessageContent)) {
            return false;//这种消息也没必要继续复读了
        }

        //避免密集复读
        long lastRepeatTimestamp = myRepeaterMapper.getLastRepeatTimestamp(groupId);
        long deltaMs = Calendar.getInstance().getTimeInMillis() - lastRepeatTimestamp;
        if (deltaMs < 1000 * 60 * 30) {
            //30分钟内只复读一次嗷
            return true;
        }

        //开始抽签
        Random random = new Random();
        if (previousMessageContent.equals(currentMessageContent)) {
            if (random.nextInt(4) != 0) return true; //如果是+1，那么1/4概率复读
            log.warn("下条群复读内容为+1消息");
        } else {
            if (random.nextInt(50) != 0) return true; //否则，2%概率复读
        }
        //命中，复读
        messageApi.sendGroupMessage(groupId, currentMessageContent, random.nextInt(3) + 2);
        //更新复读时间
        myRepeaterMapper.updateLastRepeatTimestamp(groupId, Calendar.getInstance().getTimeInMillis());
        //日志
        log.warn("复读{}群消息: {}", groupId, trimString(currentMessageContent));
        //就算复读了，也可能有别的插件需要工作
        return true;
    }

    /**
     * 通用判断文本中是否含有某些不应该出现的元素
     *
     * @param message 要判断的文本
     * @return 是否合法
     */
    private boolean isContentInappropriate(String message) {
        //不应有at人
        if (message.contains("[CQ:at")) return true;
        //不应是回复
        if (message.contains("[CQ:reply")) return true;
        //复读图片很危险
        if (message.contains("[CQ:image")) return true;
        //有链接也很危险，但这个无法隔绝大部分链接
        if (message.contains("http")) return true;
        //太长
        if (message.length() > 128) return true;
        //敏感词
        if (badWordsChecker.isContainsBadWords(message)) return true;
        return false;
    }

    /**
     * 判断字符串是否长于64，是则截断，否则原样返回
     *
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */
    private String trimString(String str) {
        if (str.length() > 64) {
            return str.substring(0, 64);
        } else {
            return str;
        }
    }

}
