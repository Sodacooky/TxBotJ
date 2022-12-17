package sodacooky.txbotj.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.core.HttpSender;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息发送相关API
 */
@Component
public class MessageApi {

    @Resource
    private HttpSender httpSender;


    /**
     * 向消息体来源发送内容
     *
     * @param cqMessageBody 来自cqhttp的消息体
     * @param content       消息内容
     */
    public void sendBackMessage(JsonNode cqMessageBody, String content, int delaySecond) {
        //获取消息类型
        String messageType = cqMessageBody.get("message_type").asText();
        //根据类型决定发送方法
        if (messageType.equals("group")) {
            sendGroupMessage(cqMessageBody.get("group_id").asLong(), content, delaySecond);
        } else if (messageType.equals("private")) {
            sendPrivateMessage(cqMessageBody.get("user_id").asLong(), content, delaySecond);
        } else {
            throw new RuntimeException("无法处理的消息源类型");
        }
    }

    /**
     * 使用错误信息格式向消息体来源发送错误信息
     *
     * @param cqMessageBody 来自cqhttp的消息体
     * @param errorMessage  错误信息内容
     */
    public void sendErrorMessage(JsonNode cqMessageBody, String errorMessage) {
        //引发错误的消息
        String causeMessageContent = cqMessageBody.get("message").asText();
        if (causeMessageContent.length() > 64) causeMessageContent = causeMessageContent.substring(0, 64) + "...";
        //构建错误消息
        String resultErrorMessage = "=== 错误 ==>" + "\n" + errorMessage + "\n" + "=== 错误来源 ==>" + "\n" + causeMessageContent;
        //send back
        sendBackMessage(cqMessageBody, resultErrorMessage, 1);
    }

    /**
     * 发送私聊消息
     *
     * @param userId  对方qq
     * @param content 消息内容
     */
    public void sendPrivateMessage(long userId, String content, int delaySecond) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", Long.toString(userId));
        params.put("message", content);
        httpSender.queueRequest(httpSender.buildUrl("send_private_msg", params), delaySecond);
    }

    /**
     * 发送群聊消息
     *
     * @param groupId 目标群号
     * @param content 消息内容
     */
    public void sendGroupMessage(long groupId, String content, int delaySecond) {
        Map<String, String> params = new HashMap<>();
        params.put("group_id", Long.toString(groupId));
        params.put("message", content);
        httpSender.queueRequest(httpSender.buildUrl("send_group_msg", params), delaySecond);
    }


}
