package sodacooky.txbotj.plugins.repeater;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@TableName("repeater")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepeaterRecord implements Serializable {
    @TableId("group_id")
    private Long groupId;
    private String lastRepeatMsg;
    private Long lastRepeatTime;
    private String lastMsgOfGroup;
    private Integer passedMsgAmount;

}
