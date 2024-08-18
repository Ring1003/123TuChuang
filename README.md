# 123TcChuang

利用123网盘的webdav存储搭建的图床
代码比较初级,有时间再优化

Demo:   http://img.wiki/

---
## 配置

| 属性                                      | 说明             | 是否必填 |
| ----------------------------------------- |----------------| -------- |
| spring.servlet.multipart.max-request-size | 限制上传图片大小，建议10m | √        |
| spring.servlet.multipart.max-file-size    | 同上             | √        |
| webdav.url                                | 要上传到哪个webdav   | √        |
| webdav.path                               | 要上传到哪个路径下      | √        |
| webdav.username                           | webdav账号，建议设置  | √        |
| webdav.password                           | webdav密码，建议设置  | √        |
| server.port                               | 程序端口，默认11223   | √        |
| localUrl.ip                               | 你的域名 或者 ip:端口  | √        |

## redis配置
redis配置文件: ```src/main/resources/config/redis.setting```

---
### 2024.08.18更新
+ 配置redis, 上传/访问 的图片会异步存到redis中24小时,提升图片访问速度
+ 异步上传: 图片上传后会直接返回访问地址,后端会异步上传到webdav和redis

----
代码参考:

https://github.com/a417707897/webdavTc


-----
待优化:

1.修改为本地存储,并异步webdav存储,(可以考虑也异步存到tgh一份)

2.访问图片时,默认返回本地存储的图片,如果报错,则返回webdav的图片
