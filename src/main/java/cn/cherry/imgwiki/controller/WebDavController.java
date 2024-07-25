package cn.cherry.imgwiki.controller;


import cn.cherry.imgwiki.util.NetworkUtils;
import cn.hutool.core.io.FileUtil;

import cn.cherry.imgwiki.config.WebDavConfig;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.http.ResponseEntity;


import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;

import java.io.InputStream;

 /**
 　*
 　* @author MengJie
 　* @date 2024-07-25 10:07:52
 　*/
@RestController
public class WebDavController {

    @Autowired
    private WebDavConfig webDavConfig;

     /**
     　* 图片上传
     　* @author MengJie
     　* @date 2024-07-24 11:07:56
     　*/
    @PostMapping("/put")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();

        //webdav账号密码
        Sardine begin = SardineFactory.begin(webDavConfig.getUsername(), webDavConfig.getPassword());

        try {
            //用户ip地址
            String ipAddress = NetworkUtils.getIpAddress(request);
            //时间戳
            long timeMillis = System.currentTimeMillis();


            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeMillis);
            SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy");
            String year = yearSdf.format(calendar.getTime());
            SimpleDateFormat monthSdf = new SimpleDateFormat("MM");
            String month = monthSdf.format(calendar.getTime());

            //存储路径拼接:url+存储目录+ip+年+月
            String path = webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year+"/"+month;
            // 检查目标文件夹是否存在
            if (!begin.exists(webDavConfig.getUrl() + webDavConfig.getSavePath())) {
                // 不存在，则创建文件夹
                begin.createDirectory(webDavConfig.getUrl() + webDavConfig.getSavePath());
            }
            if (!begin.exists(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress)) {
                // 不存在，则创建文件夹
                begin.createDirectory(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress);
            }
            if (!begin.exists(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year)) {
                // 不存在，则创建文件夹
                begin.createDirectory(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year);
            }
            if (!begin.exists(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year+"/"+month)) {
                // 不存在，则创建文件夹
                begin.createDirectory(webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year+"/"+month);
            }

            byte[] bytes = file.getBytes();

            //文件后缀
            String suffix = FileUtil.getSuffix(file.getOriginalFilename());
            //时间戳
            String millis = timeMillis + "_";
            //随机uuid
            String uuid = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 3);
            //文件名拼接: 时间戳+ip+uuid
            String newFileName = millis + ipAddress + uuid+"."+suffix;

            begin.put(path + "/" + newFileName, bytes);

            result.put("code","success");
            result.put("result", "0");
            result.put("url", "https://"+webDavConfig.getLocalIp()+"/img/"+ newFileName);
        } catch (IOException e) {
            e.printStackTrace();
            result.put("result", "1");
            result.put("code","erro");
        }

        return result;

    }

     /**
     　* 图片回显
     　* @author MengJie
     　* @date 2024-07-24 11:07:45
     　*/
    @GetMapping("/img/{imgPath:.+}")
    public ResponseEntity<Resource> getFileFromWebDav(@PathVariable String imgPath) {
        try{
            int underscoreIndex = imgPath.indexOf('_');
            long timeMillis = Long.parseLong(imgPath.substring(0, underscoreIndex));

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeMillis);
            SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy");
            String year = yearSdf.format(calendar.getTime());
            SimpleDateFormat monthSdf = new SimpleDateFormat("MM");
            String month = monthSdf.format(calendar.getTime());

            // 获取第一个下划线的位置
            int firstUnderscoreIndex = imgPath.indexOf("_");
            // 获取最后一个下划线的位置
            int lastUnderscoreIndex = imgPath.lastIndexOf("_");

            // 截取第一个下划线和最后一个下划线之间的内容
            String ipAddress = imgPath.substring(firstUnderscoreIndex + 1, lastUnderscoreIndex);


            Sardine begin = SardineFactory.begin(webDavConfig.getUsername(), webDavConfig.getPassword());

            String path = webDavConfig.getUrl() + webDavConfig.getSavePath() +"/"+ipAddress+"/"+year+"/"+month;

            InputStream inputStream = begin.get(path + "/" + imgPath);

            String mimeType = URLConnection.guessContentTypeFromName(imgPath);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(mimeType))
                    .body(new InputStreamResource(inputStream));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }


    }




}
