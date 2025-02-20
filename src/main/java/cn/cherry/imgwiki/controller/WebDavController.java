package cn.cherry.imgwiki.controller;


import cn.cherry.imgwiki.config.LocalCacheConfig;
import cn.cherry.imgwiki.util.NetworkUtils;
import cn.hutool.core.io.FileUtil;
import cn.cherry.imgwiki.config.WebDavConfig;
import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.core.JsonToken;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.SocketException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;

import java.util.logging.Level;

/**
 　*
 　* @author MengJie
 　* @date 2024-07-25 10:07:52
 　*/
@RestController
@Log
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

        // 创建Sardine实例，使用WebDAV的账号和密码进行身份验证
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
                uploadToWebDAV(begin, path, newFileName, bytes);
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
     * 将文件上传到WebDAV服务器
     *
     * @param path        上传的目标路径
     * @param newFileName 新文件名
     * @param bytes       文件的字节数组
     */
    public void uploadToWebDAV(Sardine begin,String path, String newFileName, byte[] bytes) {

        int maxRetries = 3; // 最大重试次数
        int attempt = 0; // 当前尝试次数
        boolean success = false; // 上传是否成功的标志

        // 尝试上传文件，直到成功或达到最大重试次数
        while (attempt < maxRetries && !success) {
            try {
                // 执行文件上传
                begin.put(path + "/" + newFileName, bytes);
                success = true; // 上传成功
                System.out.println("上传webdav成功: " + path + "/" + newFileName);
            } catch (SocketException e) {
                attempt++;
                System.err.println("网络异常，上传webdav失败，尝试次数：" + attempt + "，错误信息：" + e.getMessage());
                if (attempt >= maxRetries) {
                    System.err.println("达到最大重试次数，上传失败。");
                }
                // 休眠一段时间，避免频繁重试
                try {
                    Thread.sleep(3000); // 休眠3秒
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                }
            } catch (IOException e) {
                System.err.println("上传webdav失败，IO错误信息：" + e.getMessage());
                break; // 遇到其他IO异常，直接退出
            }
        }
    }


     /**
     　* 图片回显
     　* @author MengJie
     　* @date 2024-07-24 11:07:45
     　*/
    @GetMapping("/img/{imgPath:.+}")
    public ResponseEntity<Resource> getFileFromWebDav(@PathVariable String imgPath) {
        try{
            //4.设置webdav的用户名密码
            Sardine begin = SardineFactory.begin(webDavConfig.getUsername(), webDavConfig.getPassword());
            String basePath = webDavConfig.getUrl() + webDavConfig.getSavePath();


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

            //5.拼接图片的真实存储路径
            String path = basePath +filePath;

            //从本地缓存路径获取图片
            if (localCacheConfig.getPath() != null && localCacheConfig.getPath().length() > 0){
                //拼接本地缓存路径
                String localPath = localCacheConfig.getPath() + filePath + "/" + imgPath;
                // 获取文件对象
                File localFile = new File(localPath);
                //// 判断本地缓存文件是否存在且大小大于0（防止空文件或损坏文件）
                if (localFile.exists() && localFile.length() > 0){
                    // 检查图片文件的完整性
                    boolean isValidImage = false;
                    try (FileInputStream fis = new FileInputStream(localFile)) {
                        byte[] magic = new byte[8];  // 读取前8个字节用于判断图片格式
                        int read = fis.read(magic);

                        if (read >= 2) {  // 至少需要读取2个字节才能判断
                            // JPEG文件头: FF D8
                            // PNG文件头: 89 50 4E 47 0D 0A 1A 0A
                            // GIF文件头: 47 49 46 38
                            isValidImage =
                                    // JPEG检查
                                    (magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8) ||
                                            // PNG检查
                                            (magic[0] == (byte) 0x89 && magic[1] == (byte) 0x50 &&
                                                    magic[2] == (byte) 0x4E && magic[3] == (byte) 0x47) ||
                                            // GIF检查
                                            (magic[0] == (byte) 0x47 && magic[1] == (byte) 0x49 &&
                                                    magic[2] == (byte) 0x46 && magic[3] == (byte) 0x38);
                        }
                    } catch (IOException e) {
                        System.err.println("检查图片文件完整性时发生错误: " + localPath +";"+ e.getMessage());
                        isValidImage = false;
                    }
                    if (isValidImage){
                        System.out.println("本地缓存路径存在: " + localPath);
                        inputStream = FileUtil.getInputStream(localPath);

                        ThreadUtil.execute(() -> {
                            String requestPath = path + "/" + imgPath;
                            System.out.println("请求的WebDAV路径: " + requestPath);

                            try (InputStream inputStream1 = begin.get(requestPath)) {
                                // 检查InputStream是否为null
                                if (inputStream1 == null) {
                                    // 如果返回null，表示文件不存在，进行上传
                                    uploadToWebDAV( begin, path, imgPath, FileUtil.readBytes(localPath));
                                } else {
                                    // 文件存在，处理逻辑
                                    System.out.println("文件已存在，无需上传: " + imgPath);
                                }
                            } catch (IOException e) {
                                // 捕获IOException，表示可能是404错误或其他IO异常
                                System.out.println("文件404未找到，准备上传: " + imgPath);
                                uploadToWebDAV( begin, path, imgPath, FileUtil.readBytes(localPath));
                            }
                        });

                        return ResponseEntity.ok()
                                .contentType(MediaType.valueOf(mimeType))
                                .body(new InputStreamResource(inputStream));
                    }else {
                        System.out.println("本地缓存路径存在,但文件不完整: " + localPath);
                    }


                }
                System.out.println("本地缓存路径不存在: " + localPath);
            }
            System.out.println("从webdav中获取图片信息流:"+path + "/" + imgPath);
            //从webdav中获取图片信息流
            inputStream = begin.get(path + "/" + imgPath);
            // 复制输入流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();

            // 创建两个新的输入流，一个用于返回，一个用于异步写入
            ByteArrayInputStream responseStream = new ByteArrayInputStream(baos.toByteArray());
            ByteArrayInputStream cacheStream = new ByteArrayInputStream(baos.toByteArray());


            //异步将获取到的图片写入本地缓存文件夹
            ThreadUtil.execute(() ->{
                try {
                    System.out.println("异步将获取到的图片写入本地缓存文件夹");
//                    FileUtil.writeFromStream(inputStream, localCacheConfig.getPath() + filePath + "/" + imgPath);
                    FileUtil.writeFromStream(cacheStream, localCacheConfig.getPath() + filePath + "/" + imgPath);
                    System.out.println("异步写入完成");
                }catch (Exception e){
                    System.err.println("写入本地缓存失败:"+e.getMessage());
                }finally {
                    IOUtils.closeQuietly(cacheStream);
                }
            });
            // 关闭原始输入流和字节数组输出流
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(baos);

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(mimeType))
                    .body(new InputStreamResource(responseStream));
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }


    }




}
