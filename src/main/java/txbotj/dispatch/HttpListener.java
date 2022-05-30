package txbotj.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 用于监听来自go-cqhttp的post请求，将请求内容的Json字符串转化为Jackson的Json对象
 * 并把消息字符串传给MessageDispatcher。
 */
public class HttpListener implements HttpHandler {

    /**
     * Handle the given request and generate an appropriate response.
     * See {@link HttpExchange} for a description of the steps
     * involved in handling an exchange.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws NullPointerException if exchange is <code>null</code>
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //判断请求方式，虽然一定是post
        if (!exchange.getRequestMethod().toLowerCase().equals("post")) return;
        //读取内容到字符串
        ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
        exchange.getRequestBody().transferTo(stringStream);
        //转化为JacksonDOM对象
        JsonNode dom = objectMapper.readTree(stringStream.toString());
        //给予http响应，因为接下来的步骤将会很耗时
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().close();
        //传递给MessageDispatcher
        MessageDispatcher.httpHandlerCallback(dom);
    }

    //只是不用每次创建
    ObjectMapper objectMapper = new ObjectMapper();
}
