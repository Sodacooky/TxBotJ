package sodacooky.txbotj.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求发送
 * 附带参数拼接便利方法
 */
@Component
@Data
public class HttpSender {

    //内部任务使用的线程池
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

    //cqhttp监听地址，默认为http://127.0.0.1:5700，末尾不要加'/'
    private String hostAddress = "http://127.0.0.1:5700";

    /**
     * 将url请求放到队列中，每个请求将等待2秒后执行，该方法不阻塞
     *
     * @param requestUrl 请求的URL
     */
    public Future<JsonNode> queueRequest(String requestUrl, int delaySecond) {
        //添加任务
        return executorService.schedule(() -> sendRequest(requestUrl), delaySecond, TimeUnit.SECONDS);
        //将Future返回给上层，是否要阻塞处理响应由上层决定
    }


    /**
     * 使用自身hostAddress为前缀，将请求方法和参数拼接
     *
     * @param method     请求方法，如"send_private_msg"
     * @param parameters 参数
     * @return 拼接后的字符串
     */
    public String buildUrl(String method, Map<String, String> parameters) {
        return buildCustomUrl(hostAddress, method, parameters);
    }

    /**
     * 使用Header代替http://xxx部分，将请求方法和参数拼接
     *
     * @param header     "http://xxx"的部分，结尾不要有斜杠
     * @param method     请求方法，如"send_private_msg"
     * @param parameters 参数
     * @return 拼接后的字符串
     */
    public String buildCustomUrl(String header, String method, Map<String, String> parameters) {
        //虽然约定了不能以斜杠结尾，还是判断一下并移除吧
        while (header.endsWith("/")) {
            //删除直到不以斜杠为结尾
            header = header.substring(0, header.length() - 1);
        }
        //接下来把参数拼接成 ?key=val&key=val...
        StringBuilder paramsBuilder = new StringBuilder("?");
        parameters.forEach((k, v) -> {
            paramsBuilder.append(k).append("=").append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            paramsBuilder.append("&");
        });
        //最终把三者拼起来
        return header + "/" + method + paramsBuilder.toString();
    }

    /**
     * 等待两秒然后发送请求，返回结果的Json
     *
     * @param requestUrl 请求url
     * @return 来自cqhttp的结果json
     */
    private JsonNode sendRequest(String requestUrl) {
        //生成URL请求
        URL url = null;
        try {
            url = new URL(requestUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //发送URL请求
        URLConnection urlConnection = null;
        String result = null;
        try {
            //连接
            urlConnection = url.openConnection();
            urlConnection.connect();
            //获取响应数据
            ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
            urlConnection.getInputStream().transferTo(stringStream);
            result = stringStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //结果为Json字符串，转换为JsonNode
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
