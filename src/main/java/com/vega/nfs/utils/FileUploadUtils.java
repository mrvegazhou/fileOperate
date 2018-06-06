package com.vega.nfs.utils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.*;

import com.vega.nfs.config.FileConfig;
import com.vega.nfs.constant.FileConst;
import com.vega.nfs.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;

import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class FileUploadUtils implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadUtils.class);

    private static ApplicationContext applicationContext = null;

    public FileUploadUtils() {
    }

    public static String basefolder;

    public static Integer downUrlTimeOut;

    /**
     * 获取主目录下的文件夹
     * @return
     */
    public static List<String> getDocumentsList() {
        String bf = getBasefolderConf();
        if(bf==null || bf.isEmpty()) {
            return null;
        }
        File file = new File(bf);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        return Arrays.asList(directories);
    }

    /**
     * 通过文件夹获取内部的所有文件和属性
     * @param directory
     * @return
     */
    public static List<Map> readDir(String directory) {
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();
        List<Map> filesList = new ArrayList<Map>();
        if(listOfFiles==null) {
            return filesList;
        }
        try {
            for(File file: listOfFiles){
                Map<String, Object> fileMap = new HashMap<String, Object>();
                StringBuilder sb = new StringBuilder();
                sb.append(directory).append(File.separator).append(file.getName());
                String fullPath = sb.toString();
                BasicFileAttributes attr = Files.readAttributes(Paths.get(fullPath), BasicFileAttributes.class);
                fileMap.put("fileName", file.getName());
                fileMap.put("fileSize", attr.size());
                fileMap.put("creationTime", attr.creationTime().toString());
                fileMap.put("isDirectory", attr.isDirectory());
                fileMap.put("isRegularFile", attr.isRegularFile());
                filesList.add(fileMap);
            }
            return filesList;
        }catch (IOException e) {
            e.printStackTrace();
            return filesList;
        }
    }

    public static String getDirectoryPath(String file) {
        String baseDirectory = getBasefolderConf();
        if(!file.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            //获取文件夹路径
            String fileDir = getDirPath(file);
            sb.append(baseDirectory).append(File.separator).append(fileDir);
            return sb.toString();
        }
        return baseDirectory;
    }

    public static String getDirectoryPath(FileInformation fileInfo) {
        return getDirectoryPath(fileInfo.getPathDir());
    }

    public static String createDirectory(FileInformation fileInfo) {
        return createDirectory(fileInfo.getPathDir());
    }

    public static String createDirectory(String path) {
        String fullPath = getDirectoryPath(path);
        File file = new File(fullPath);
        if(!file.exists()) {
            file.mkdirs();
        }
        return file.getPath();
    }

    //判断路径下是否存在同名的文件名
    public static boolean checkExistSomeFile(String pathDir, String fileName, String extName) {
        String fullPathDir = FileUploadUtils.getDirectoryPath(pathDir);
        File fileTmp = null;
        if (extName.equals("")) {
            fileTmp = new File(fullPathDir+File.separator+fileName);
        } else {
            fileTmp = new File(fullPathDir+File.separator+fileName+"."+extName);
        }
        if (fileTmp.isFile()) {
            return true;
        }
        return false;
    }

    /**
     * 获取配置文件中的文件存储主目录
     * @return
     */
    public static String getBasefolderConf() {
        if (basefolder==null){
            FileConfig fileConfig = applicationContext.getBean(FileConfig.class);
            String  authBasepath = JwtUser.getUserDetail().getBasepath();
            String basefolderTmp = fileConfig.getBasefolder();
            StringBuilder sb = new StringBuilder();
            sb.append(basefolderTmp).append(File.separator).append(authBasepath);
            basefolder = sb.toString();
        }
        return basefolder;
    }

    /**
     * 获取url下载的过期时间
     * @return
     */
    public static Integer getDownUrlTimeOut() {
        if (downUrlTimeOut==null){
            FileConfig fileConfig = applicationContext.getBean(FileConfig.class);
            downUrlTimeOut = fileConfig.getDownUrlTimeOut();
        }
        return downUrlTimeOut;
    }
    /**
     * 上传文件
     * @param fileInfo
     * @return
     * @throws IOException
     */
    public static boolean saveFileData(FileInformation fileInfo) throws IOException {
        String fullPath = getFullPath(fileInfo);
        Path p = Paths.get(fullPath);
//        byte[] in2b = FileUploadUtils.inputStream2bytes(fileInfo.getFileData());
//        Files.write(p, in2b);
        Files.copy(fileInfo.getFileData(), p, REPLACE_EXISTING);
        fileInfo.getFileData().close();
        return true;
    }

    public static byte[] inputStream2bytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer=new byte[1024*4];
        int n=0;
        while ((n=in.read(buffer))!=-1) {
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }

    public static String getFullPath(FileInformation fileInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDirectoryPath(fileInfo)).append(File.separator).append(fileInfo.getFileId()).append(".").append(fileInfo.getExt());
        return sb.toString();
    }

    public static boolean checkIsFile(String filepath) {
        File file = new File(filepath);
        return file.isFile();
    }

    /**
     * 获取文件大小
     * @param file
     * @return
     * @throws IOException
     */
    public static long getFileSize(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            return file.length();
        } else {
            return 0;
        }
    }

    /**
     * 判断文件后缀名是否合理
     * @param ext
     * @param exts
     * @return
     */
    public static boolean checkFileExt(String ext, String exts) {
        String tmp = exts.replace("，", ",");
        String[] extArr = tmp.split(",");
        for (String type : extArr) {
            if (type.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    // 通过字符串路径获取文件夹路径
    public static String getDirPath(String path) {
        Path filePath = Paths.get(path);
        String filePathTmp = filePath.toString();
        if(-1==filePathTmp.lastIndexOf('.')) {
            return StringUtils.strip(path, File.separator);
        } else {
            return StringUtils.strip(filePathTmp.substring(0, filePathTmp.lastIndexOf(File.separator)), File.separator);
        }
    }

    /**
     * 判断文件夹下是否存在文件
     * @param path
     * @return
     */
    public static boolean checkDirHasFiles(String path) {
        File file = new File(path);
        File[] listFiles = file.listFiles();
        if(listFiles.length > 0){
            return true;
        } else {
            return false;
        }
    }

    /**
     * 为文件自定义属性赋值
     * @param fileInfo
     * @param name
     * @param value
     * @throws IOException
     */
    public static void setFileAttr(FileInformation fileInfo, String name, String value) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(getDirectoryPath(fileInfo)).append(File.separator).append(fileInfo.getFileId()).append(".").append(fileInfo.getExt());
        String fullPath = sb.toString();
        Path path = Paths.get(fullPath);
        UserDefinedFileAttributeView view =  Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        view.write(name, ByteBuffer.wrap(value.getBytes()));
    }

    /**
     * 获取文件的自定义属性值
     * @param path
     * @param name
     * @return
     * @throws IOException
     */
    public static String getFileAttr(String path, String name) throws IOException {
        Path file = Paths.get(path);
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file,UserDefinedFileAttributeView.class);
        ByteBuffer buf = ByteBuffer.allocate(view.size(name));
        view.read(name, buf);
        buf.flip();
        String value = Charset.defaultCharset().decode(buf).toString();
        return value;
    }

    /**
     * 判断文件内容是否为图片
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isImage(File file) throws IOException {
        BufferedImage bi = ImageIO.read(file);
        if(bi == null){
            return false;
        }
        return true;
    }

    /**
     * 判断图片文件的后缀
     * @param filename
     * @return
     */
    public static boolean checkImg(String filename) {
        String tmpName = "";
        if ((filename != null) && (filename.length() > 0)) {
            int i = filename.lastIndexOf('.');

            if ((i > 0) && (i < (filename.length() - 1))) {
                tmpName = (filename.substring(i+1)).toLowerCase();
            }
        }
        String imgeArray[][] = { { "bmp", "0" }, { "dib", "1" },
                { "gif", "2" }, { "jfif", "3" }, { "jpe", "4" },
                { "jpeg", "5" }, { "jpg", "6" }, { "png", "7" },
                { "tif", "8" }, { "tiff", "9" }, { "ico", "10" } };
        for (int i = 0; i < imgeArray.length; i++) {
            if (imgeArray[i][0].equals(tmpName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 直接判断字符串是否为文件夹，并删除
     * @param folderPath
     * @return
     */
    public static int deleteFolderNoCheckFiles(String folderPath) {
        Path folder = Paths.get(folderPath);
        String folderTmp = folder.toString();
        if(folderTmp==FileUploadUtils.getBasefolderConf()) {
            return FileConst.DEL_BASEDIR_ERR.getCode();
        }
        File dir = new File(folderTmp);
        if(!dir.exists()) {
            return FileConst.DEL_NOTDIR_ERR.getCode();
        } else {
            //删除文件夹
            File[] files = dir.listFiles();
            for(int i=0; i<files.length; i++) {
                new File(files[i].getAbsolutePath()).delete();
            }
            boolean success = dir.delete();
            if(success) {
                return 1;
            } else {
                return FileConst.DEL_DIR_ERR.getCode();
            }
        }
    }

    /**
     * 删除文件夹
     * @param folderPath
     * @return
     * @throws IOException
     */
    public static int deleteFolder(String folderPath) throws IOException {
        //判断删除的是否为主目录
        Path folder = Paths.get(folderPath);
        String folderTmp = folder.toString();
        folderTmp = StringUtils.strip(folderTmp, File.separator);
        if(folderTmp==FileUploadUtils.getBasefolderConf()) {
            return FileConst.DEL_BASEDIR_ERR.getCode();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getBasefolderConf()).append(File.separator).append(folderPath);
        String fullPathDir = sb.toString();
        File dir = new File(fullPathDir);
        if(!dir.isDirectory()) {
            return FileConst.DEL_NOTDIR_ERR.getCode();
        } else {
            String[] files = dir.list();
            if(files.length>0) {
                return FileConst.DEL_DIR_EXIST_ERR.getCode();
            } else {
                //删除文件夹
                boolean success = dir.delete();
                if(success) {
                    return 1;
                } else {
                    return FileConst.DEL_DIR_ERR.getCode();
                }

            }
        }
    }

    public static void deleteFile(String pathFile) {
        Path folder = Paths.get(pathFile);
        String pathFileTmp = folder.toString();
        File filePath = new File(pathFileTmp);
        if (filePath.exists()) {
            filePath.delete();
        }
    }

    /**
     * 拼成完整路径，并判断文件是否是文件，返回File对象
     * @param filePath
     * @return
     */
    public static File getRealFilePath(String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(getBasefolderConf()).append(File.separator).append(filePath);
        String fullPathDir = sb.toString();
        Path fullPathDirTmp = Paths.get(fullPathDir);
        String fullPathDirStr = fullPathDirTmp.toString();
        //判断是否为文件
        boolean flag = FileUploadUtils.checkIsFile(fullPathDirStr);
        if(!flag) {
            return null;
        }
        File file = new File(fullPathDirStr);
        return file;
    }

    public static String getFileNameNoExt(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            int slash = filename.lastIndexOf(File.separator);
            int len = filename.length();
            if ((dot >-1) && (dot < len)) {
                return filename.substring(slash+1, dot);
            }
        }
        return "";
    }

    /**
     * 通过url下载文件 返回url文件类
     * @param urlStr
     * @param pathDir
     * @param newFileName
     * @param checkFileSize
     * @param maxSize
     * @return
     * @throws IOException
     */
    public static URLFileInfomation urlDownload(String urlStr, String pathDir, String newFileName, boolean checkFileSize, int maxSize) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setConnectTimeout(getDownUrlTimeOut().intValue());
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept-Encoding", "identity");
        //防止屏蔽程序抓取而返回403错误
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

        conn.connect();
        //先连接一次，解决跳转下载
        conn.getResponseCode();

        String tmpUrl = conn.getURL().toString();
        String fileName = tmpUrl.substring(tmpUrl.lastIndexOf("/")+1);
        String extName = "";
        if (fileName.lastIndexOf(".")>0){
            extName = fileName.substring(fileName.lastIndexOf(".")+1);
        }
        if (extName==null || extName.length()>4 || extName.indexOf("?")>-1) {
            fileName = conn.getHeaderField("Content-Disposition");
            if(fileName!=null || fileName.indexOf("filename")>=0){
                fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename")+10,fileName.length()-1),"UTF-8");
                extName = fileName.substring(fileName.lastIndexOf(".")+1);
            }
        }
        String tmpFileName = "";
        if (fileName.indexOf(".")!= -1) {
            tmpFileName = fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            tmpFileName = fileName;
        }
        if (checkFileSize==true && conn.getResponseCode()==200) {
            long length = conn.getContentLength();
            if (length > maxSize) {
                URLFileInfomation urlInfo = new URLFileInfomation();
                urlInfo.setIsBigFile(true);
                urlInfo.setFileSize(length);
                urlInfo.setFileName(tmpFileName);
                urlInfo.setFileId(newFileName);
                urlInfo.setPathDir(pathDir);
                urlInfo.setExt(extName);
                urlInfo.setUrl(urlStr);
                if (!extName.equals(""))
                    urlInfo.setExt(extName);
                return urlInfo;
            }
        }

        long size = Long.parseLong(conn.getHeaderField("Content-Length"));
        if (checkFileSize==true) {
            if (size > maxSize) {
                URLFileInfomation urlInfo = new URLFileInfomation();
                urlInfo.setIsBigFile(true);
                urlInfo.setFileSize(size);
                urlInfo.setFileName(tmpFileName);
                urlInfo.setFileId(newFileName);
                urlInfo.setPathDir(pathDir);
                urlInfo.setUrl(urlStr);
                if (!extName.equals(""))
                    urlInfo.setExt(extName);
                return urlInfo;
            }
        }
        InputStream inputStream = conn.getInputStream();
        URLFileInfomation urlInfo;
        if (newFileName.isEmpty()) {

            urlInfo = new URLFileInfomation(
                    inputStream,
                    tmpFileName,
                    pathDir,
                    extName,
                    urlStr,
                    false
            );
        } else {
            urlInfo = new URLFileInfomation(
                    inputStream,
                    newFileName,
                    tmpFileName,
                    pathDir,
                    extName,
                    urlStr,
                    false
            );
        }
        urlInfo.setFileSize(size);
        conn.disconnect();
        return urlInfo;
    }

    public static long getUrlFileSize(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        long size = conn.getContentLength();
        conn.getInputStream().close();
        return size;
    }

    public static String removeExtName(String filename) {
        int dot = filename.lastIndexOf('.');
        if ((dot >-1) && (dot < (filename.length()))) {
            return filename.substring(0, dot);
        }
        return filename;
    }

    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }

    /**
     * 通过整型获取提示字符串
     * @param key
     * @return
     */
    public static String getDeleteErrorMap(Integer key) {
        Map<Integer, String> deleteErrorMap = new HashMap<Integer, String>();
        deleteErrorMap.put(FileConst.DEL_DIR_EXIST_ERR.getCode(), "The folder contains files that cannot be deleted");
        deleteErrorMap.put(FileConst.DEL_NOTDIR_ERR.getCode(), "Folder does not exist");
        deleteErrorMap.put(FileConst.DEL_BASEDIR_ERR.getCode(), "The home directory cannot be deleted");
        deleteErrorMap.put(FileConst.DEL_DIR_ERR.getCode(), "Delete directory failed");
        deleteErrorMap = Collections.unmodifiableMap(deleteErrorMap);
        return deleteErrorMap.get(key);
    }

    public static File mergeFiles(ProgressInfo info, File fileBlockProgressFolderObj, String fileBlockSuffix, String newFileUrl) throws IOException {
        ArrayList<File> sequenceFiles = new ArrayList<>();
        for (FileBlockProgress blockProcess : info.getProcess()) {
            String fileBlockName = String.format("%s/%d.%s",fileBlockProgressFolderObj.getPath(), blockProcess.getNumber(), fileBlockSuffix);
            File file = new File(fileBlockName);
            if (file.exists()) {
                sequenceFiles.add(file);
            } else {
                throw new RuntimeException(String.format("The %s block file was not found.", blockProcess.getNumber()));
            }
        }
        //创建合并文件夹
        File outputFile = new File(newFileUrl);
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        FileChannel outChannel = null;
        FileChannel inChannel = null;
        try {
            outChannel = new FileOutputStream(outputFile).getChannel();
            for (File file : sequenceFiles) {
                inChannel = new FileInputStream(file).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
                inChannel.close();
                file.delete();
            }
            outChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inChannel!=null)
                inChannel.close();
            if (outChannel!=null)
                outChannel.close();
        }
        return outputFile;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(FileUploadUtils.applicationContext == null){
            FileUploadUtils.applicationContext  = applicationContext;
        }
    }

    public static Properties readProperties() throws IOException {
        Resource resource = new ClassPathResource("/application.properties");
        Properties prop = PropertiesLoaderUtils.loadProperties(resource);
        return prop;
    }

    /**
     * 判断是否为整型
     * @param value
     * @return
     */
    public static boolean isValidInt(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
