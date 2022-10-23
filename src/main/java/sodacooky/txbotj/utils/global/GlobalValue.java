package sodacooky.txbotj.utils.global;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class GlobalValue {

    @Resource
    private GlobalValueMapper globalValueMapper;

    /**
     * 根据key获取value值
     *
     * @param key key
     * @return 如果没有记录或为null值
     */
    public String readValue(String key) {
        GlobalValueObject globalValueObject = globalValueMapper.selectById(key);
        if (null == globalValueObject) return null;
        else return globalValueObject.getValue();
    }

    /**
     * 修改或新增数据
     *
     * @param key   key
     * @param value new value
     */
    public void setValue(String key, String value) {
        GlobalValueObject globalValueObject = globalValueMapper.selectById(key);
        if (null == globalValueObject) {
            //不存在，新增
            globalValueMapper.insert(new GlobalValueObject(key, value));
        } else {
            //存在，更改
            LambdaUpdateWrapper<GlobalValueObject> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GlobalValueObject::getKey, key).set(GlobalValueObject::getValue, value);
            globalValueMapper.update(null, wrapper);
        }
    }

    /**
     * 移除数据
     *
     * @param key key
     * @return 如果本来就没有该数据，返回FALSE
     */
    public boolean remove(String key) {
        LambdaQueryWrapper<GlobalValueObject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GlobalValueObject::getKey, key);
        if (globalValueMapper.exists(wrapper)) {
            return globalValueMapper.delete(wrapper) > 0;
        } else {
            return false;
        }
    }

}
