package com.vega.nfs.service;

import com.vega.nfs.constant.FileConst;
import com.vega.nfs.model.URLFileInfomation;
import com.vega.nfs.utils.FileUploadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

@Component
public class URLDownloadThread {
    private static final Logger logger = LoggerFactory.getLogger(URLDownloadThread.class);

    @Value("${nfs.url.threadCount:5}")
    private int threadCount;

    @Value("${nfs.url.urlPrefix:_#}")
    public static String urlPrefix;

    private int limitBlock = 10485760;

    private CountDownLatch latch;

    public static int downUrlTimeOut = 10000;

    public static int downUrlReadTimeOut = 600000;

    private static String lock = "lock";

    public URLFileInfomation uploadFile(URLFileInfomation urlInfo, Integer threadNum)  {
        long length = urlInfo.getFileSize();
        String extName = urlInfo.getExt();
        String tmpPath = urlInfo.getPathDir();
        String newFileName = urlInfo.getFileId();
        String url = urlInfo.getUrl();
        if (threadNum!=null && threadNum>0) {
            //判断每个线程下载的字节数 小于10M
            if (length/threadNum<this.limitBlock) {
                throw new RuntimeException(FileConst.URL_THREAD_COUNT_ERR.toString());
            } else {
                this.threadCount = threadNum;
            }

        }

        Thread mainThread = Thread.currentThread();
        try {
            CountDownLatch latch = new CountDownLatch(this.threadCount);
            String path = FileUploadUtils.createDirectory(tmpPath);
            String filePath = "";
            if (extName==null && extName.equals("")) {
                filePath = String.format("%s%s%s", path, File.separator, newFileName);
            } else {
                filePath = String.format("%s%s%s%s", path, File.separator, newFileName, "."+extName);
            }
            File file = new File(filePath);
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.setLength(length);
            raf.close();
            long size = length / this.threadCount;
            for (int i = 0; i < this.threadCount; i++) {
                long startIdx = i * size;
                long endIdx = (i + 1) * size - 1;
                if(i == this.threadCount - 1) {
                    endIdx = length - 1;
                }
                URLDownLoadPartThread thread = new URLDownLoadPartThread(mainThread, url, path, newFileName, extName, startIdx, endIdx, i, latch, this.threadCount);
                thread.start();
            }
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urlInfo;
    }

    private class URLDownLoadPartThread extends Thread {
        long startIdx;
        long endIdx;
        int tid;
        String extName;
        String fileName;
        String path;
        String url;
        CountDownLatch latch;
        int count;
        String tmpPathPartDir;
        Thread mainThread;

        public URLDownLoadPartThread(Thread mainThread, String url, String path, String fileName, String extName, long startIdx, long endIdx, int tid, CountDownLatch latch, int count) {
            super();
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.tid = tid;
            this.fileName = fileName;
            this.path = path;
            this.url = url;
            this.latch = latch;
            this.count = count;
            this.mainThread = mainThread;
            this.extName = extName;
        }

        @Override
        public void run() {
            int x = 0;
            boolean errFlag = false;

            while (x < 3) {
                try {
                    String downFilePath = String.format("%s%s%s", this.path, File.separator, this.fileName);
                    if (this.extName!=null && !this.extName.equals("")) {
                        downFilePath = downFilePath + "." + this.extName;
                    }

                    String tmpPathPartDir = String.format("%s%s%s%s%s", this.path, File.separator, URLDownloadThread.urlPrefix, this.fileName, "#"+this.extName);
                    File tmpPathPartDirObj = new File(tmpPathPartDir);
                    if (!tmpPathPartDirObj.exists()) {
                        tmpPathPartDirObj.mkdirs();
                    }
                    this.tmpPathPartDir = tmpPathPartDir;
                    String tmpPathPartFile = String.format("%s%s%s.part#%s", tmpPathPartDir, File.separator, this.fileName, this.tid);
                    // 记录进度的临时文件
                    File progressPartFile = new File(tmpPathPartFile);
                    if (!progressPartFile.exists()) {
                        File dirs = new File(progressPartFile.getParent());
                        if (!dirs.exists()) {
                            dirs.mkdirs();
                        }
                        progressPartFile.createNewFile();

                        FileOutputStream output = null;
                        try {
                            output = new FileOutputStream(progressPartFile);
                            output.write("0".getBytes());
                            output.flush();
                            output.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (output != null) {
                                try {
                                    output.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(FileConst.CLOSE_FILE_ERR.toString(), e);
                                }
                            }
                        }
                    }
                    FileInputStream fis = null;
                    BufferedReader br = null;
                    try {
                        fis = new FileInputStream(progressPartFile);
                        br = new BufferedReader(new InputStreamReader(fis));
                        startIdx += Integer.parseInt(br.readLine());
                        fis.close();
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fis!=null) {
                            fis.close();
                        }
                        if (br!=null) {
                            br.close();
                        }
                    }

                    System.out.println("线程" + tid + "的下载区间是：" + startIdx + "---" + endIdx);

                    URL url = new URL(this.url);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(URLDownloadThread.downUrlTimeOut);
                    conn.setReadTimeout(URLDownloadThread.downUrlReadTimeOut);
                    conn.setRequestProperty("Range", String.format("bytes=%s-%s",  startIdx, endIdx));
                    if (conn.getResponseCode() == 206) {
                        InputStream is = conn.getInputStream();
                        byte[] b = new byte[1024];
                        int len = 0;
                        int total = 0;
                        File file = new File(downFilePath);
                        RandomAccessFile raf = null;
                        try {
                            raf = new RandomAccessFile(file, "rwd");
                            raf.seek(startIdx);
                            while ((len = is.read(b)) != -1) {
                                raf.write(b, 0, len);
                                total += len;
                            }
                            raf.close();
                            is.close();
                        } catch (Exception ioe) {
                            ioe.printStackTrace();
                        } finally {
                            if (raf!=null) {
                                raf.close();
                            }
                            if (is!=null) {
                                is.close();
                            }
                        }

                        RandomAccessFile progressRaf = new RandomAccessFile(progressPartFile, "rwd");
                        progressRaf.write((total + "").getBytes());
                        progressRaf.close();
                    }
                    conn.disconnect();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    x += 1;
                    errFlag = true;
                    continue;
                }
            }
            latch.countDown();
            synchronized (URLDownloadThread.lock) {
                if (latch.getCount()==0) {
                    int flag = FileUploadUtils.deleteFolderNoCheckFiles(this.tmpPathPartDir);
                    logger.info(flag+"");
                    logger.info(this.tmpPathPartDir);
                    Assert.isTrue(flag>=0, FileConst.getEnumDesc((int)flag));
                }
            }
//            if (true==errFlag && latch.getCount()==0) {
//                if (new File(this.tmpPathPartDir).exists()) {
//                    int flag = FileUploadUtils.deleteFolderNoCheckFiles(this.tmpPathPartDir);
//                    if (flag < 0) {
//                        throw new RuntimeException(FileConst.getEnumDesc((int) flag));
//                    }
//                }
//                StringBuilder sb = new StringBuilder();
//                sb.append(this.path);
//                sb.append(File.separator);
//                sb.append(this.fileName);
//                if (this.extName!=null && this.extName.equals("")) {
//                    sb.append(this.extName);
//                }
//                FileUploadUtils.deleteFile(sb.toString());
//                boolean isFlag = FileUploadUtils.checkDirHasFiles(this.path);
//                if (!isFlag) {
//                    try {
//                        FileUploadUtils.deleteFolder(this.path);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e.getMessage());
//                    }
//                }
////                Thread.currentThread().interrupt();
////                throw new RuntimeException(FileConst.URL_DOWNLOAD_ERR.toString());
//            } else {
////                throw new RuntimeException(FileConst.URL_DOWNLOAD_SUCCESS.toString());
//            }
        }
    }

}
