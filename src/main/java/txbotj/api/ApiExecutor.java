package txbotj.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

//cqhttp的API
public class ApiExecutor {

    public static void sendPrivateMessage(long user_id, String content) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", Long.toString(user_id));
        params.put("message", content);
        JsonNode result = HttpSender.sendHttpRequest(HttpSender.buildUrl(address, "send_private_msg", params));
        LoggerFactory.getLogger(ApiExecutor.class).info("/send_private_msg to {}\n\t{}\n\tresult: {}", user_id, content, result.toString());
    }

    public static void sendGroupMessage(long group_id, String content) {
        Map<String, String> params = new HashMap<>();
        params.put("group_id", Long.toString(group_id));
        params.put("message", content);
        JsonNode result = HttpSender.sendHttpRequest(HttpSender.buildUrl(address, "send_group_msg", params));
        LoggerFactory.getLogger(ApiExecutor.class).info("/send_group_msg to {}\n\t{}\n\tresult: {}", group_id, content, result.toString());
    }

    //go-cqhttp的监听地址，默认为本地http://127.0.0.1:5700
    public static String address = "http://127.0.0.1:5700";

}
