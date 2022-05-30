package txbotj.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 用于向go-cqhttp发送请求
 */
public class HttpSender {

    //请求方法拼接
    public static String buildUrl(String host, String method, Map<String, String> parameters) {
        //remove the "/"
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        //
        return host + "/" + method + buildParametersString(parameters);
    }

    //底层参数拼接实现
    private static String buildParametersString(Map<String, String> parameters) {
        StringBuilder stringBuilder = new StringBuilder("?");
        parameters.forEach((k, v) -> {
            stringBuilder.append(k).append("=").append(v);
            stringBuilder.append("&");
        });
        return stringBuilder.toString();
    }

    //发送请求
    public static JsonNode sendHttpRequest(String urlString) {
        //speed limit
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //convert to url object
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //execute request
        URLConnection urlConnection = null;
        String result = null;
        try {
            urlConnection = url.openConnection();
            urlConnection.connect();
            //the result
            ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
            urlConnection.getInputStream().transferTo(stringStream);
            result = stringStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        try {
            return objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
}
