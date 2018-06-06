package com.vega.nfs.service;

import com.vega.nfs.constant.FileConst;
import com.vega.nfs.exception.FileUploadException;
import com.vega.nfs.model.FileInformation;
import com.vega.nfs.model.FileMetaData;
import com.vega.nfs.utils.FileUploadUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service("fileUploadData")
public class FileUploadData implements IFileUploadData {

    /**
     * 只是文件存储逻辑
     * @param fileInfo
     * @return
     * @throws FileUploadException
     */
    @Override
    public int uploadFile(FileInformation fileInfo) throws FileUploadException {
        FileUploadUtils.createDirectory(fileInfo);
        try{
            //判断文件夹下是否存在相同命名的文件
            String fullFilePath = FileUploadUtils.getFullPath(fileInfo);
            if(FileUploadUtils.checkIsFile(fullFilePath)) {
                return FileConst.UPLOAD_FILE_EXISTS_ERR.getCode();
            }
            boolean flag = FileUploadUtils.saveFileData(fileInfo);
            if(!flag) {
                return FileConst.UPLOAD_FILE_ERR.getCode();
            }
            // 插入自定义属性
            // FileUploadUtils.setFileAttr(fileInfo, "user.filename", fileInfo.getFileName());
            return FileConst.UPLODA_FILE_SUCCESS.getCode();
        } catch(IOException ex){
            throw new FileUploadException("Error occurred: "+ex.getMessage());
        }
    }

    @Override
    public FileInformation getFileInfo(String filepath) throws FileUploadException {
        return null;
    }

    @Override
    public List<Map> readDir(String dir) throws FileUploadException {
        String baseDir = FileUploadUtils.getBasefolderConf();
        String filePathTmp;
        if(dir!=null || !dir.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(baseDir).append(File.separator).append(dir);
            String fullPath = sb.toString();
            Path filePath = Paths.get(fullPath);
            filePathTmp = filePath.toString();
        } else {
            filePathTmp = baseDir;
        }
        //判断是否为存在的文件夹
        File fullPathDir = new File(filePathTmp);
        if(!fullPathDir.isDirectory()) {
            return null;
        }
        List<Map> fileList = FileUploadUtils.readDir(filePathTmp);
        return fileList;
    }

    @Override
    public boolean deleteFile(String filePath) throws FileUploadException {
        //获取主目录
        String baseDir = FileUploadUtils.getBasefolderConf();
        StringBuilder sb = new StringBuilder();
        sb.append(baseDir).append(File.separator).append(filePath);
        String fullPath = sb.toString();
        boolean isFile = FileUploadUtils.checkIsFile(fullPath);
        if(isFile) {
            File file = new File(fullPath);
            if(file.delete()){
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public String deleteFolder(String dirPath) throws FileUploadException, IOException {
        int flag = FileUploadUtils.deleteFolder(dirPath);
        String errorMsg = "";
        if(flag<0) {
            errorMsg = FileUploadUtils.getDeleteErrorMap(flag);
        }
        return errorMsg;
    }
}
