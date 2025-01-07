package cn.cherry.imgwiki.controller;


import cn.cherry.imgwiki.config.LocalCacheConfig;
import cn.cherry.imgwiki.util.NetworkUtils;
import cn.hutool.core.io.FileUtil;

import cn.cherry.imgwiki.config.WebDavConfig;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.db.nosql.redis.RedisDS;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.http.ResponseEntity;


import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import redis.clients.jedis.Jedis;

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
    /**本地缓存配置*/
    @Autowired
    private LocalCacheConfig localCacheConfig;


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


            // 获取当前时间的Calendar对象
            Calendar calendar = Calendar.getInstance();
            // 设置Calendar对象的时间，单位为毫秒
            calendar.setTimeInMillis(timeMillis);
            // 创建格式化对象，用于提取年份
            SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy");
            // 格式化时间，提取年份
            String year = yearSdf.format(calendar.getTime());
            // 创建格式化对象，用于提取月份
            SimpleDateFormat monthSdf = new SimpleDateFormat("MM");
            // 格式化时间，提取月份
            String month = monthSdf.format(calendar.getTime());

            // 构建完整存储路径(存储路径拼接:url+存储目录+ip+年+月)
            String basePath = webDavConfig.getUrl() + webDavConfig.getSavePath();
            String[] pathElements = {ipAddress, year, month};
            StringBuilder currentPath = new StringBuilder(basePath);

            // 创建目录
            for (String element : pathElements) {
                // 逐个拼接路径元素，形成当前路径
                currentPath.append("/").append(element);
                // 检查当前路径的目录是否存在
                if (!begin.exists(currentPath.toString())) {
                    // 如果目录不存在，则创建该目录
                    begin.createDirectory(currentPath.toString());
                }
            }
            // 最终完整路径
            String path = currentPath.toString();

            byte[] bytes = file.getBytes();

            //文件后缀
            String suffix = FileUtil.getSuffix(file.getOriginalFilename());
            //后缀改为小写
            if (!StringUtils.isEmpty(suffix)){
                suffix = suffix.toLowerCase();
            }
            //时间戳
            String millis = timeMillis + "_";
            //随机uuid
            String uuid = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 3);
            //文件名拼接: 时间戳+ip+uuid
            String newFileName = millis + ipAddress + uuid+"."+suffix;


            // 创建完整路径
            StringBuilder fullPath = new StringBuilder();
            for (String element : pathElements) {
                fullPath.append("/").append(element);
            }
            //写入到本地缓存路径
            FileUtil.writeBytes(bytes, localCacheConfig.getPath() +fullPath+"/"+ newFileName);

            //上传到webdav
            ThreadUtil.execute(() ->{
                try {
                    begin.put(path + "/" + newFileName, bytes);
                } catch (IOException e) {
                    try {
                        begin.put(path + "/" + newFileName, bytes);
                    } catch (IOException ex) {
                        try {
                            begin.put(path + "/" + newFileName, bytes);
                        } catch (IOException exc) {
                            System.err.println("上传erbdav失败:"+e.getMessage());
                        }
                    }
                }
            });

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
            //图片流
            InputStream inputStream ;
            //1.获取图片名中的时间戳
            int underscoreIndex = imgPath.indexOf('_');
            long timeMillis = Long.parseLong(imgPath.substring(0, underscoreIndex));
            //2.根据时间戳转换成年月
            Calendar calendar = Calendar.getInstance();
            // 设置Calendar对象的时间，单位为毫秒
            calendar.setTimeInMillis(timeMillis);
            // 创建格式化对象，用于提取年份
            SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy");
            // 格式化时间，提取年份
            String year = yearSdf.format(calendar.getTime());
            // 创建格式化对象，用于提取月份
            SimpleDateFormat monthSdf = new SimpleDateFormat("MM");
            // 格式化时间，提取月份
            String month = monthSdf.format(calendar.getTime());

            // 获取第一个下划线的位置
            int firstUnderscoreIndex = imgPath.indexOf("_");
            // 获取最后一个下划线的位置
            int lastUnderscoreIndex = imgPath.lastIndexOf("_");

            // 3.获取图片名称中的ip地址, 截取第一个下划线和最后一个下划线之间的内容
            String ipAddress = imgPath.substring(firstUnderscoreIndex + 1, lastUnderscoreIndex);
            //获取文件类型
            String mimeType = URLConnection.guessContentTypeFromName(imgPath);
            String filePath = "/"+ipAddress+"/"+year+"/"+month;

            //从本地缓存路径获取图片
            if (localCacheConfig.getPath() != null && localCacheConfig.getPath().length() > 0){
                //拼接本地缓存路径
                String localPath = localCacheConfig.getPath() + filePath + "/" + imgPath;
                //判断本地缓存路径是否存在
                if (FileUtil.exist(localPath)){
                    inputStream = FileUtil.getInputStream(localPath);
                    //todo 异步判断webdav上有没有这个图片,没有的话就上传一下

                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(mimeType))
                            .body(new InputStreamResource(inputStream));
                }
            }

            //4.设置webdav的用户名密码
            Sardine begin = SardineFactory.begin(webDavConfig.getUsername(), webDavConfig.getPassword());
            //5.拼接图片的真实存储路径
            String path = webDavConfig.getUrl() + webDavConfig.getSavePath() +filePath;

            //从webdav中获取图片信息流
            inputStream = begin.get(path + "/" + imgPath);

            //异步将获取到的图片写入本地缓存文件夹
            ThreadUtil.execute(() ->{
                try {
                    FileUtil.writeFromStream(inputStream, localCacheConfig.getPath() + filePath + "/" + imgPath);
                }catch (Exception e){
                    System.err.println("写入本地缓存失败:"+e.getMessage());
                }
            });

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(mimeType))
                    .body(new InputStreamResource(inputStream));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }


    }




}
