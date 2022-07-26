package sodacooky.txbotj.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 注册监听根URL，使用Spring的Controller监听来自cqhttp的post请求，
 * 将收到的请求，（自动换转为的Json）分发给各个插件进行处理
 */
@RestController
public class MessageDispatcher {

    @Autowired
    private PluginContainer pluginContainer; //插件容器

    @PostMapping("/")
    public void response(@RequestBody JsonNode requestJson) {
        List<IPlugin> plugins = pluginContainer.getPlugins();
        if (!requestJson.get("post_type").asText().equalsIgnoreCase("message")) {
            //不是消息类型，忽略
            return;
        }
        //群聊消息，私聊消息，还是什么消息
        switch (requestJson.get("message_type").asText()) {
            case "group":
                plugins.forEach((onePlugin) -> onePlugin.onGroupMessage(requestJson));
                break;
            case "private":
                plugins.forEach((onePlugin) -> onePlugin.onPrivateMessage(requestJson));
                break;
            default:
                break;
        }
    }


}
