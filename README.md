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



---
Demo部署在: [破碎工坊](https://www.crash.work/aff/QLDAMTOD "破碎工坊")

![图片](https://imgs.uninull.cn/i/16e5cf980b6a922975424dfc5cefbd77/66a10d3880352.png)



----
代码参考:

https://github.com/a417707897/webdavTc
