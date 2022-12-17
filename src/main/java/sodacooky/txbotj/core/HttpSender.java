package sodacooky.txbotj.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * HTTP请求发送
 * 附带参数拼接便利方法
 */
@Component
public class HttpSender {

    private ExecutorService executorService;//并发容器
    @Resource
    private ObjectMapper objectMapper;//JsonNode转换工具
    public String hostAddress = "http://127.0.0.1:5700";//cqhttp监听地址，默认为http://127.0.0.1:5700，末尾不要加'/'

    //construct
    public HttpSender() {
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * 将url请求放到队列中，每个请求将等待2秒后执行，该方法不阻塞
     *
     * @param requestUrl 请求的URL
     */
    public Future<JsonNode> queueRequest(String requestUrl, int delaySecond) {
        //添加任务
        return executorService.submit(() -> {
            Thread.sleep(delaySecond * 1000L);
            return sendRequest(requestUrl);
        });
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
     * @param header     http://xxx的部分，结尾不要有斜杠
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
     * 返回内部的线程池，以供释放或其他操作
     *
     * @return ExecutorService
     */
    public ExecutorService getInternalThreadPool() {
        return executorService;
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
            return objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
