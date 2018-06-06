package com.vega.nfs.model;

import java.io.Serializable;
import java.util.UUID;

public class FileMetaData implements Serializable {
    private static final long serialVersionUID = 1L;

    String fileId;
    String fileName;
    String pathDir;
    long fileSize;
    String ext;
    String creationDate;

    public FileMetaData() {
    }

    public FileMetaData(String fileName, String pathDir, String ext) {
        this(UUID.randomUUID().toString(), fileName, pathDir, ext);
    }

    public FileMetaData(String fileId, String fileName, String pathDir, String ext) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.pathDir = pathDir;
        this.ext = ext;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long l) {
        this.fileSize = l;
    }

    public String getPathDir() {
        return pathDir;
    }
    public void setPathDir(String pathDir) {
        this.pathDir = pathDir;
    }
    public String getFileId() {
        return fileId;
    }
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    public String getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    public String getExt() {
        return ext;
    }
    public void setExt(String ext) {
        this.ext = ext;
    }

    public ProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(ProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    private ProgressInfo progressInfo;
}
