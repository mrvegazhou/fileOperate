package com.vega.nfs.service;

import com.vega.nfs.exception.FileUploadException;
import com.vega.nfs.model.FileInformation;
import com.vega.nfs.model.FileMetaData;
import com.vega.nfs.model.URLFileInfomation;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IFileUploadService {
    FileInformation localFileSave(HttpServletRequest request,
                                  MultipartFile file,
                                  String pathDir,
                                  String newFileName,
                                  Long fileSize,
                                  Integer blockIndex,
                                  Integer blockTotal,
                                  String ext,
                                  String exts) throws IOException, FileUploadException, CloneNotSupportedException;
    URLFileInfomation urlFileSave(String url, String pathDir, String newFileName, String ext, String exts, Integer threadCount) throws IOException, FileUploadException;
    List<Map> readDir(String dir) throws FileUploadException;
    boolean deleteFile(String filePath) throws FileUploadException;
    String deleteFolder(String dirPath) throws FileUploadException, IOException;
}
