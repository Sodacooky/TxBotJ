package txbotj.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 提供回调函数，被HttpHandler调用,
 * 并按消息类型触发插件的onXXXMessage回调函数。
 */
public class MessageDispatcher {

    //接收来自HttpHandler的消息内容字符串
    public static void httpHandlerCallback(JsonNode message) {
        //只是个触发其他函数的入口
        dispatchMessage(message);
        //可以插入一些统计，或多线程化
    }

    //根据消息类型，触发插件的方法
    private static void dispatchMessage(JsonNode message) {
        //判断类型，switch
        switch (message.get("post_type").asText()) {
            case "message":
                //char message dispatch
                dispatchChat(message);
                break;
            default:
                //other
                break;
            //可以拓展对其他进行支持
        }

        //调用，并把json对象带上
    }

    private static void dispatchChat(JsonNode message) {
        //判断群聊私聊
        switch (message.get("message_type").asText()) {
            case "group":
                plugins.forEach((k, v) -> v.onGroupMessage(message));
                break;
            case "private":
                plugins.forEach((k, v) -> v.onPrivateMessage(message));
                break;
            default:
                //other
                break;
        }
    }

    //在Plugin构造函数中注册的插件
    public static final Map<String, Plugin> plugins = new HashMap<>();
}
