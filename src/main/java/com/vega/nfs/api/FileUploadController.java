package com.vega.nfs.api;

import com.vega.nfs.constant.FileConst;
import com.vega.nfs.exception.FileUploadException;
import com.vega.nfs.model.FileInformation;
import com.vega.nfs.model.FileMetaData;
import org.apache.commons.lang3.StringUtils;
import com.vega.nfs.service.FileUploadService;
import com.vega.nfs.service.IUserService;
import com.vega.nfs.service.UserRepository;
import com.vega.nfs.utils.FileUploadUtils;
import com.vega.nfs.utils.JSONResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/file")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${nfs.fileBlockFolder}")
    String fileBlockFolder;

    @Value("${nfs.url.urlPrefix:_#}")
    private String urlPrefix;

    @Autowired
    FileUploadService service;

    @Autowired
    UserRepository userRep;

    @Autowired
    IUserService userService;

    /**
     * 单文件上传
     * @param file
     * @param pathDir
     * @return
     * @throws FileUploadException
     * @throws IOException
     */
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value="/localFileUpload", method= RequestMethod.POST, consumes = "multipart/form-data")
    public String uploadLocalFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "name", required = false) String newFileName,
            @RequestParam(value = "path", required = false) String pathDir,
            @RequestParam(value = "ext", required = false) String ext,
            @RequestParam(value = "exts", required = false) String exts,
            @RequestParam(value = "fileSize", required = false) Long fileSize,   //文件总大小 当url方式下载的时候可以不需要传fileSize参数
            @RequestParam(value = "blockIndex", required = false) Integer blockIndex,   // 当前分片数
            @RequestParam(value = "blockTotal", required = false) Integer blockTotal,   // 分片总数
            HttpServletRequest request
    ) throws FileUploadException, IOException {
        if (file==null) {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`file` parameter is null", null);
        }
        if (pathDir.startsWith("..") || pathDir.startsWith(this.fileBlockFolder) || pathDir.startsWith(this.urlPrefix)) {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), FileConst.FILE_PATH_ERR.toString(), null);
        } else {
            pathDir = pathDir.trim();
        }

        if (newFileName!=null) {
            newFileName = newFileName.trim();
            if (newFileName.equals("")) {
                return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`name` parameter is null", null);
            }
            if (newFileName.indexOf("\\")!= -1 || newFileName.indexOf("/")!= -1) {
                return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`name` parameter is illegal", null);
            }
        }

        if (exts!=null) {
            exts = exts.trim();
            if (!exts.equals(""))
                exts = exts.trim().toLowerCase();
        }

        FileInformation fileData = service.localFileSave(request, file, pathDir, newFileName, fileSize, blockIndex, blockTotal, ext, exts);
        if (fileData==null) {
            return JSONResult.reResultString(HttpStatus.NOT_FOUND.toString(), FileConst.UPLOAD_FILE_NULL_ERR.toString(), null);
        }

        //logger.info("ACTION:fileUpload,IP:{},上传文件名:{},路径:{},自定义名称:{},user:{}", request.getRemoteAddr(), fileName, pathDir, newFileName, JwtUser.getUserDetail().getUsername());
        FileMetaData fileMeta = new FileMetaData();
        fileMeta.setFileSize(fileData.getFileSize());
        fileMeta.setExt(fileData.getExt());
        fileMeta.setFileId(fileData.getFileId());
        fileMeta.setFileName(fileData.getFileName());
        fileMeta.setPathDir(fileData.getPathDir());
        fileMeta.setProgressInfo(fileData.getProgressInfo());
        fileMeta.setCreationDate(Long.toString(System.currentTimeMillis()));
        return JSONResult.reResultString(HttpStatus.OK.toString(), null, fileMeta);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value="/urlFileUpload", method= RequestMethod.POST)
    public String uploadUrlFile(
            @RequestParam(value = "name", required = false) String newFileName,
            @RequestParam(value = "path", required = false) String pathDir,
            @RequestParam(value = "ext", required = false) String ext,
            @RequestParam(value = "threadNum", required = false) Integer threadNum,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "exts", required = false) String exts
    ) throws FileUploadException, IOException {
        if (url==null) {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`url` parameter is null", null);
        }
        if (pathDir.startsWith("..") || pathDir.startsWith(this.fileBlockFolder) || pathDir.startsWith(this.urlPrefix)) {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), FileConst.FILE_PATH_ERR.toString(), null);
        } else {
            pathDir = pathDir.trim();
        }

        if (newFileName!=null) {
            newFileName = newFileName.trim();
            if (newFileName.equals("")) {
                return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`name` parameter is null", null);
            }
            if (newFileName.indexOf("\\")!= -1 || newFileName.indexOf("/")!= -1) {
                return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`name` parameter is illegal", null);
            }
        }

        if (exts!=null) {
            exts = exts.trim();
            if (!exts.equals(""))
                exts = exts.trim().toLowerCase();
        }

        FileInformation fileData = service.urlFileSave(url, pathDir, newFileName, ext, exts, threadNum);
        // 判断url文件的大小和已经下载的文件是否大小一样
        StringBuilder sb = new StringBuilder();
        sb.append(FileUploadUtils.getDirectoryPath(pathDir));
        sb.append(File.separator);
        sb.append(fileData.getFileId());
        if (fileData.getExt() != null && !fileData.getExt().equals("")) {
            sb.append(".");
            sb.append(fileData.getExt());
        }

        if (fileData==null) {
            return JSONResult.reResultString(HttpStatus.NOT_FOUND.toString(), FileConst.UPLOAD_FILE_NULL_ERR.toString(), null);
        }

        FileMetaData fileMeta = new FileMetaData();
        fileMeta.setFileSize(fileData.getFileSize());
        fileMeta.setExt(fileData.getExt());
        fileMeta.setFileId(fileData.getFileId());
        fileMeta.setFileName(fileData.getFileName());
        fileMeta.setPathDir(fileData.getPathDir());
        fileMeta.setProgressInfo(fileData.getProgressInfo());
        fileMeta.setCreationDate(Long.toString(System.currentTimeMillis()));
        return JSONResult.reResultString(HttpStatus.OK.toString(), null, fileMeta);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(path = "/processStatus", method = RequestMethod.POST)
    public String processInfo(
            @RequestParam(value = "name", required = true) String fileName,
            @RequestParam(value = "path", required = true) String pathDir,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "isLocalFlag", required = true) String isLocalFlag
    ) throws IOException {
        if (fileName.trim().equals("")) {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`fileName` parameter is null!", null);
        }
        Map<String, Long> map;
        if (isLocalFlag.equals("1")) {
            map = service.getReadLocalProgressInfo(fileName, pathDir);
        } else if (isLocalFlag.equals("0")) {
            if (url==null || url.equals("")) {
                return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`url` parameter is null!", null);
            }
            map = service.getReadURLProgressInfo(fileName, pathDir, url);
        } else {
            return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), "`isLocalFlag` parameter is error!", null);
        }
        if (map!=null)
            return JSONResult.reResultString(HttpStatus.OK.toString(), null, map);
        else
            return JSONResult.reResultString(HttpStatus.SEE_OTHER.toString(), "Confirm whether it is a large file or existing file?", null);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(path = "/deleteFolder", method = RequestMethod.POST)
    public Map<String, Object> deleteFolder(
            @RequestParam(value = "folder", required = true) String folderPath,
            HttpServletRequest request
    ) {
//        logger.info("ACTION:deleteFolder,IP:{},folder:{},user:{}", request.getRemoteAddr(), folderPath, JwtUser.getUserDetail().getUsername());

        Map<String, Object> map = new HashMap<>();
        if(folderPath.isEmpty()) {
            map.put("status", HttpStatus.BAD_REQUEST.toString());
            map.put("error", "`folder` parameter is null!");
            return map;
        } else {
            try{
                String result = service.deleteFolder(folderPath);
                if(!result.isEmpty()) {
                    map.put("status", HttpStatus.BAD_REQUEST.toString());
                    map.put("error", result);
                }
            } catch (FileUploadException e) {
                map.put("status", HttpStatus.BAD_REQUEST.toString());
                map.put("error", e.toString());
                return map;
            } catch(IOException e) {
                map.put("status", HttpStatus.BAD_REQUEST.toString());
                map.put("error", e.toString());
                return map;
            }
        }
        map.put("status", HttpStatus.OK.toString());
        map.put("result", folderPath);
        return map;
    }

    /**
     *  删除文件
     * @throws IOException
     */
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(path = "/deleteFiles", method = RequestMethod.POST)
    public Map<String, Object> deleteFiles(
            @RequestParam(value = "files[]", required = true) String[] filePaths,
            HttpServletRequest request
    ) throws IOException  {
        StringBuilder filesSb = new StringBuilder();
        Map<String, Object> map = new HashMap<String, Object>();
        if(filePaths.length==0) {
            map.put("status", HttpStatus.BAD_REQUEST.toString());
            map.put("error", "`files` parameter is null");
            return map;
        }
        List<String> fileList = new ArrayList<>();
        for (int i = 0; i < filePaths.length; i++) {
            if(!filePaths[i].isEmpty()) {
                try{
                    boolean flag = service.deleteFile(filePaths[i]);
                    if(flag) {
                        fileList.add(filePaths[i]);
                        String fileDir = FileUploadUtils.getDirectoryPath(filePaths[i]);
                        //组合成url下载的临时文件夹路径
                        String fileName = FileUploadUtils.getFileNameNoExt(filePaths[i]);
                        String urlTmpDir = String.format("%s%s_%s", fileDir, File.separator, fileName);
                        File fileTmpDirObj = new File(urlTmpDir);
                        if (fileTmpDirObj.exists()) {
                            FileUploadUtils.deleteFolderNoCheckFiles(urlTmpDir);
                        }
                        //组合本地下载大文件的临时文件夹路径
                        String fileBlockProgressFolder = String.format("%s%s%s%s", FileUploadUtils.getBasefolderConf(), File.separator, fileDir, File.separator, this.fileBlockFolder);
                        File fileBlockProgressFolderObj = new File(fileBlockProgressFolder);
                        if (fileBlockProgressFolderObj.exists()) {
                            FileUploadUtils.deleteFolderNoCheckFiles(fileBlockProgressFolder);
                        }
                    }
                } catch (FileUploadException e) {
                    continue;
                }
                filesSb.append(filePaths[i]).append(",");
            }
        }

//        logger.info("ACTION:deleteFiles,IP:{},files:{},user:{}", request.getRemoteAddr(), filesSb.toString(), JwtUser.getUserDetail().getUsername());

        map.put("status", HttpStatus.OK.toString());
        map.put("result", fileList);
        return map;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value="/lookupFiles", method= RequestMethod.GET)
    public Map<String, Object> getFileDetails(HttpServletRequest request) throws FileUploadException, IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        String dir = request.getParameter("dir");
        if (dir==null) {
            dir = "";
        }
        List<Map> fileList = service.readDir(dir);
        if(fileList==null) {
            map.put("status", HttpStatus.BAD_REQUEST.toString());
            map.put("result", "the folder path does not exist");
        } else {
            map.put("status", HttpStatus.OK.toString());
            map.put("result", fileList);
        }

//        logger.info("ACTION:lookupFiles,IP:{},files:{},user:{}", request.getRemoteAddr(), dir, JwtUser.getUserDetail().getUsername());

        return map;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping("/download")
    public void download(
            @RequestParam(value = "filePath", required = true) String filePath,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

//        logger.info("ACTION:download,IP:{},filePath:{},user:{}", request.getRemoteAddr(), filePath, JwtUser.getUserDetail().getUsername());
        service.download(filePath, request, response);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value="/downloadZip", method = RequestMethod.POST)
    public void downloadZip(
            @RequestParam(value = "filePath[]", required = true) String[] filePaths,
            @RequestParam(value = "zipName", required = true) String zipName,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (zipName.equals("")) {
            throw new RuntimeException("`zipName` is empty!");
        }
        int len = filePaths.length;
        if (len==0) {
            throw new RuntimeException("`filePath[]` is empty!");
        }
        Set<String> set = new HashSet<String>();
        List<String> errFiles =new ArrayList<>();
        for (int i=0; i<len; i++) {
            File srcfile = FileUploadUtils.getRealFilePath(filePaths[i]);
            if (srcfile==null) {
                errFiles.add(filePaths[i]);
            } else {
                set.add(filePaths[i]);
            }
        }
        //判断是否包含错误文件
        if (errFiles.size()>0) {
            String errStr = String.format("The file paths：`%s` are Illegal paths！", StringUtils.join(errFiles, ","));
            throw new RuntimeException(errStr);
        }
        errFiles = null;

        String[] fileNewPaths =  set.toArray(new String[1]);
        String zipFile = service.zipFiles(fileNewPaths, zipName);
        service.download(zipFile, request, response);
        File zipObj = FileUploadUtils.getRealFilePath(zipFile);
        if (zipObj!=null) {
            zipObj.delete();
        }
    }

    private final ResourceLoader resourceLoader;
    @Autowired
    public FileUploadController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/img")
    @ResponseBody
    public ResponseEntity<?> getImg(@RequestParam(value = "path", required = true) String path) {
        try {
            boolean isImg = FileUploadUtils.checkImg(path);
            if (!isImg) {
                return ResponseEntity.notFound().build();
            }
            String baseDir = FileUploadUtils.getBasefolderConf();
            StringBuilder sb = new StringBuilder();
            sb.append(baseDir);
            sb.append(File.separator);
            Path filePath = Paths.get(path);
            sb.append(filePath);
            String strPath = sb.toString();
            File file = new File(strPath);
            if (file.exists()) {
                boolean flag = FileUploadUtils.isImage(file);
                if (!flag) {
                    return ResponseEntity.notFound().build();
                } else {
                    return ResponseEntity.ok(resourceLoader.getResource("file:"+strPath));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}