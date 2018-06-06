package com.vega.nfs.model;

import java.util.ArrayList;
import java.util.List;

public class ProgressInfo {
    /**
     * 分片大小
     */
    long blockSize;

    /**
     * 名称
     */
    String fileName;

    /**
     * 总大小
     */
    long totalSize;

    /**
     * 已经上传的大小
     */
    long uploadedSize;

    /**
     * 是否完成
     */
    boolean finish;

    /**
     * 每个片的进度
     */
    List<FileBlockProgress> process = new ArrayList<FileBlockProgress>();
    public ProgressInfo addProcess(FileBlockProgress blockProcess) {
        process.add(blockProcess);
        return this;
    }
    public List<FileBlockProgress> getProcess() {
        return process;
    }
    public void setProcess(List<FileBlockProgress> process) {
        this.process = process;
    }

    public long getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getUploadedSize() {
        return uploadedSize;
    }

    public void setUploadedSize(long uploadedSize) {
        this.uploadedSize = uploadedSize;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

}
