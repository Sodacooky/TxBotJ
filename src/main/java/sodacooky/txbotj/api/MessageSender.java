package sodacooky.txbotj.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.core.HttpSender;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息发送相关API
 */
@Component
public class MessageSender {

    @Autowired
    private HttpSender httpSender;

    /**
     * 发送私聊消息
     *
     * @param userId  对方qq
     * @param content 消息内容
     */
    public void sendPrivateMessage(long userId, String content) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", Long.toString(userId));
        params.put("message", content);
        httpSender.queueRequest(httpSender.buildUrl("send_private_msg", params));
    }

    /**
     * 发送群聊消息
     *
     * @param groupId 目标群号
     * @param content 消息内容
     */
    public void sendGroupMessage(long groupId, String content) {
        Map<String, String> params = new HashMap<>();
        params.put("group_id", Long.toString(groupId));
        params.put("message", content);
        httpSender.queueRequest(httpSender.buildUrl("send_group_msg", params));
    }

}
