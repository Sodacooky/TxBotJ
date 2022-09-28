package sodacooky.txbotj.plugins.repeater;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.BadWordsChecker;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Random;

/**
 * 进行群消息复读和私聊复读。
 * 群消息按概率复读并检查是否有敏感词，私聊消息全部复读也检查敏感词。
 */
@Component
public class Repeater implements IPlugin {

    @Resource
    private BadWordsChecker badWordsChecker; //敏感词判断工具
    @Resource
    private MessageApi messageApi;
    @Resource
    private RepeaterMapper repeaterMapper; //复读机所用数据库操作

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
     * @param message 消息json
     * @return 如果包含敏感词等，返回false
     */
    @Override
    public boolean onPrivateMessage(JsonNode message) {
        //获取消息信息
        String msgContent = message.get("message").asText();
        long msgUser = message.get("user_id").asLong();
        //检查
        if (isContentInappropriate(msgContent)) {
            return false;//这种消息也没必要继续复读了
        }
        //送回
        messageApi.sendPrivateMessage(msgUser, msgContent, new Random().nextInt(3) + 1);
        LoggerFactory.getLogger(Repeater.class).warn("复读了私聊消息到 {} : {}", msgUser, trimString(msgContent));
        return true;//就算复读了，也可能有别的插件需要工作
    }

    /**
     * 群消息复读，
     * 收到消息时会更新数据库
     *
     * @param message 消息json
     * @return 如果包含敏感词等，返回false
     */
    @Override
    public boolean onGroupMessage(JsonNode message) {
        //获取消息信息
        String msgContent = message.get("message").asText();
        long groupId = message.get("group_id").asLong();
        //检查数据库
        if (repeaterMapper.isExist(groupId) == 0) {
            //不存在，新建
            repeaterMapper.createNew(groupId);
        }
        //判断+1可能性
        boolean existPlusOne = repeaterMapper.getLastMessageContent(groupId).equals(msgContent);
        //更新数据库，上一条消息
        repeaterMapper.updateLastMessageContent(groupId, msgContent);
        //检查
        if (isContentInappropriate(msgContent)) {
            return false;//这种消息也没必要继续复读了
        }
        //避免密集复读
        long lastRepeatTimestamp = repeaterMapper.getLastRepeatTimestamp(groupId);
        long deltaMs = Calendar.getInstance().getTimeInMillis() - lastRepeatTimestamp;
        if (deltaMs < 1000 * 60 * 30) {
            //30分钟内只复读一次嗷
            return true;
        }
        //开始抽签
        Random random = new Random();
        //如果是+1，那么1/4概率复读
        if (existPlusOne) {
            if (random.nextInt(4) != 0) {
                return true;
            }
            LoggerFactory.getLogger(Repeater.class).warn("下条复读为+1消息");
        } else { //否则，1%概率复读
            if (random.nextInt(100) != 0) {
                return true;
            }
        }
        //命中，复读
        messageApi.sendGroupMessage(groupId, msgContent, random.nextInt(3) + 1);
        //更新复读时间
        repeaterMapper.updateLastRepeatTimestamp(groupId, Calendar.getInstance().getTimeInMillis());
        LoggerFactory.getLogger(Repeater.class).warn("复读了群消息到 {} : {}", groupId, trimString(msgContent));
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
