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
public class RepeaterPlugin implements IPlugin {

    @Resource
    private BadWordsChecker badWordsChecker; //敏感词判断工具
    @Resource
    private MessageApi messageApi;
    @Resource
    private RepeaterRecordService repeaterRecordService;

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
        //检查，不复读不合适的消息
        if (isContentInappropriate(cqMessageBody.get("message").asText())) return false;
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

        //检查数据库是否有当前群的记录，如果没有要创新新的
        if (repeaterRecordService.getById(groupId) == null) {
            repeaterRecordService.save(new RepeaterRecord(groupId, "", 0L, "", 0));
        }

        //获取当前群的复读相关信息
        RepeaterRecord matchedRecord = repeaterRecordService.getById(groupId);
        //暂时数据库上一条消息
        String previousMessageContent = matchedRecord.getLastMsgOfGroup();
        //更新未复读消息数量
        matchedRecord.setPassedMsgAmount(matchedRecord.getPassedMsgAmount() + 1);

        //是否为+1消息的抽签算法并不相同
        boolean isHitRepeat;
        Random random = new Random();
        //抽签
        if (previousMessageContent.equals(currentMessageContent)) {
            //如果老的消息等于当前消息，为+1消息，对于此消息1/4概率复读
            isHitRepeat = random.nextInt(4) == 0;
        } else {
            //对于一般消息，实行复读控制
            //复读的概率为 min[未复读消息数量 * 0.5%,50%]
            double hitPossibility = matchedRecord.getPassedMsgAmount() * 0.005;
            hitPossibility = Math.min(hitPossibility, 0.5);
            isHitRepeat = random.nextInt((int) (1 / hitPossibility)) == 0;
        }
        //检查，如果是不合适小消息没必要继续复读了
        if (isContentInappropriate(currentMessageContent)) isHitRepeat = false;
        //如果消息过于密集(小于十分钟)，忽略
        if (Calendar.getInstance().getTimeInMillis() - matchedRecord.getLastRepeatTime() <= 600000) isHitRepeat = false;
        //如果消息是复读过的，忽略
        if (currentMessageContent.equals(matchedRecord.getLastRepeatMsg())) isHitRepeat = false;


        //如果命中了，需要将未复读消息数量清零，
        if (isHitRepeat) {
            //命中了，清零
            matchedRecord.setPassedMsgAmount(0);
            //记录复读的内容
            matchedRecord.setLastRepeatMsg(currentMessageContent);
            matchedRecord.setLastMsgOfGroup(currentMessageContent);
            //发送消息
            messageApi.sendGroupMessage(groupId, currentMessageContent, random.nextInt(3) + 2);
            //日志
            log.warn("复读{}群消息: {}", groupId, trimString(currentMessageContent));
        }
        //无论是否复读都要更新
        repeaterRecordService.update(matchedRecord, null);

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
