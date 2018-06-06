package com.vega.nfs.model;

import java.io.InputStream;
import java.net.HttpURLConnection;

public class URLFileInfomation extends FileInformation {
    public boolean getIsBigFile() {
        return isBigFile;
    }

    public void setIsBigFile(boolean isBigFile) {
        this.isBigFile = isBigFile;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isBigFile;
    public String url;

    public HttpURLConnection getConn() {
        return conn;
    }

    public void setConn(HttpURLConnection conn) {
        this.conn = conn;
    }

    public HttpURLConnection conn;


    public URLFileInfomation() {}

    public URLFileInfomation(InputStream fileData, String fileName, String pathDir, String ext, String url, boolean isBigFile) {
        super(fileData, fileName, pathDir, ext);
        this.setUrl(url);
        this.setIsBigFile(isBigFile);
    }

    public URLFileInfomation( InputStream fileData, String fileId, String fileName, String pathDir, String ext, String url, boolean isBigFile) {
        super(fileData, fileId, fileName, pathDir, ext);
        this.setUrl(url);
        this.setIsBigFile(isBigFile);
    }

}
