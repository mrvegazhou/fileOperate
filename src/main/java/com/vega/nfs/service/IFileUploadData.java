package com.vega.nfs.service;

import com.vega.nfs.exception.FileUploadException;
import com.vega.nfs.model.FileInformation;
import com.vega.nfs.model.FileMetaData;
import com.vega.nfs.utils.FileUploadUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IFileUploadData {
    int uploadFile(FileInformation fileInfo) throws FileUploadException;
    FileInformation getFileInfo(String id) throws FileUploadException;
    List<Map> readDir(String dir) throws FileUploadException;
    boolean deleteFile(String filePath) throws FileUploadException;
    String deleteFolder(String dirPath) throws FileUploadException, IOException;
}
