package sodacooky.txbotj.core;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sodacooky.txbotj.api.MessageApi;

import java.util.List;

/**
 * 注册监听根URL，使用Spring的Controller监听来自cqhttp的post请求，
 * 将收到的请求，（自动换转为的Json）分发给各个插件进行处理
 */
@Slf4j
@RestController
public class MessageDispatcher {

    //插件容器
    @Resource
    private PluginContainer pluginContainer;

    //错误消息处理
    @Resource
    private MessageApi messageApi;

    @PostMapping("/")
    public void response(@RequestBody JsonNode cqMessageBody) {
        List<IPlugin> plugins = pluginContainer.getPlugins();
        if (!cqMessageBody.get("post_type").asText().equalsIgnoreCase("message")) {
            //不是消息类型，忽略
            return;
        }
        try {
            //群聊消息，私聊消息，还是什么消息
            switch (cqMessageBody.get("message_type").asText()) {
                case "group":
                    for (IPlugin plugin : plugins) {
                        boolean ret = plugin.onGroupMessage(cqMessageBody);
                        if (!ret) return;
                    }
                    break;
                case "private":
                    for (IPlugin plugin : plugins) {
                        boolean ret = plugin.onPrivateMessage(cqMessageBody);
                        if (!ret) return;
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Error: {}\nFrom message:\n{}", e.getMessage(), cqMessageBody.toPrettyString());
            //原路返回错误消息
            messageApi.sendErrorMessage(cqMessageBody, e.getMessage());
            //抛出了异常一定不继续传递给下一个插件
        }
    }


}
