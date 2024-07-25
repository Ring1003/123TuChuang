package cn.cherry.imgwiki.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Data
public class WebDavConfig {

    @Value("${webdav.url}")
    private String url;

    @Value("${webdav.path}")
    private String savePath;

    @Value("${webdav.username}")
    private String username;

    @Value("${webdav.password}")
    private String password;


    @Value("${localUrl.ip}")
    private String localIp;

}
