package com.vega.nfs.model;

import java.io.InputStream;

public class FileInformation extends FileMetaData {
    private InputStream fileData;

    public FileInformation( InputStream fileData, String fileName, String pathDir, String ext) {
        super(fileName, pathDir, ext);
        this.setFileData(fileData);
    }

    public FileInformation( InputStream fileData, String fileId, String fileName, String pathDir, String ext) {
        super(fileId, fileName, pathDir, ext);
        this.setFileData(fileData);
    }

    public FileInformation() {
    }

    public void setFileData(InputStream fileData) {
        this.fileData = fileData;
    }

    public InputStream getFileData() {
        return fileData;
    }
}
