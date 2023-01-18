# TxBotJ

使用SpringBoot处理go-cqhttp消息的qq机器人。

目前项目已经弃坑，仅作为Java下编写qq机器人的一个例子。

# 建议

用Spring全家桶做QQ机器人，只有在多账户之类的情况下有点价值，否则你写个复读机都要拖着100M的依赖。

业务逻辑才是你的重点，其余的像HTTP服务器、消息分发、API调用之类的都是重复造轮子。

建议使用NoneBot之类的机器人框架，只需要关注业务，而且Python写起来更加轻松。

# 项目结构

根目录：

```
myapp.db        项目使用的SQLite数据库（敏感信息不提供）
myapp.db.sql    项目使用的数据库的结构
```

src/main/java/sodacooky.txbotj：

```
api/        一些gocq的api调用实现
core/       HTTP请求处理、消息分发和插件（功能）接口
utils/      一些工具，比如从数据库中读取“全局变量”（像超级用户列表）、和敏感词过滤
plugins/    你所编写的插件（功能），所有插件需要实现IPlugin接口，就会在启动时被加载
```