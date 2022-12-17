package sodacooky.txbotj.utils.managercheck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import sodacooky.txbotj.utils.global.GlobalValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理者身份检查工具
 */
@Component
public class ManagerChecker {

    @Resource
    private GlobalValue globalValue;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 判断是否是机器人管理员
     *
     * @param user_id 欲判断的qq
     * @return 是否为管理员
     */
    public boolean isManager(long user_id) {
        String manager = globalValue.readValue("manager");
        try {
            //get all manager
            JsonNode jsonNode = objectMapper.readTree(manager);
            Assert.isTrue(jsonNode.isArray(), "global.manager应为Json数组！");
            //convert to list
            List<Long> managers = new ArrayList<>();
            for (JsonNode managerIdStrNode : jsonNode) {
                managers.add(Long.parseLong(managerIdStrNode.asText()));
            }
            //check
            return managers.contains(user_id);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
