package com.vega.nfs.model;

public class FileBlockProgress {
    /**
     * 分块编号
     */
    private Integer number;

    /**
     * 当前分块正在操作，所以加锁
     */
    private boolean lock;

    /**
     * 开始当前分块相对于整个文件的开始字节
     */
    private long startByte;

    /**
     * 当前分块相对于整个文件的结束字节
     */
    private long endByte;

    /**
     * 已经上传了的字节
     */
    private long uploadedByte;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    public long getUploadedByte() {
        return uploadedByte;
    }

    public void setUploadedByte(long uploadedByte) {
        this.uploadedByte = uploadedByte;
    }
}
