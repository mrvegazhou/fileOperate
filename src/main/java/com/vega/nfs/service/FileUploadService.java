package com.vega.nfs.service;

import com.vega.nfs.config.FileConfig;
import com.vega.nfs.constant.FileConst;
import com.vega.nfs.exception.FileUploadException;
import com.vega.nfs.model.FileBlockProgress;
import com.vega.nfs.model.FileInformation;
import com.vega.nfs.model.ProgressInfo;
import com.vega.nfs.model.URLFileInfomation;
import com.vega.nfs.utils.FileUploadUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service("uploadService")
public class FileUploadService implements IFileUploadService {

    @Autowired
    IFileUploadData fileUploadData;

    @Autowired
    URLDownloadThread urlDownThread;

    @Value("${nfs.fileBlockSize}")
    long fileBlockSize;

    @Value("${nfs.fileBlockSuffix}")
    String fileBlockSuffix;

    @Value("${nfs.fileBlockFolder}")
    String fileBlockFolder;

    @Value("${nfs.zipFolder}")
    String zipFolder;

    /**
     * 处理保存文件逻辑 一些其他业务逻辑
     * @param url
     * @param pathDir
     * @param newFileName
     * @param exts
     * @return
     * @throws IOException
     * @throws FileUploadException
     */
    @Override
    public URLFileInfomation urlFileSave(String url, String pathDir, String newFileName, String ext, String exts, Integer threadCount) throws IOException, FileUploadException {
        URLFileInfomation urlInfo = this.getUrlFile(url, pathDir, newFileName, ext, exts);

        if (!urlInfo.getIsBigFile()) {
            int flag = fileUploadData.uploadFile(urlInfo);
            Assert.isTrue(flag>0, FileConst.getEnumDesc(flag));
            return urlInfo;
        } else {
            // 大文件需要多线程上传
            return urlDownThread.uploadFile(urlInfo, threadCount);
        }
    }

    @Override
    public FileInformation localFileSave(HttpServletRequest request,
                                         MultipartFile file,
                                         String pathDir,
                                         String newFileName,
                                         Long fileSize,
                                         Integer blockIndex,
                                         Integer blockTotal,
                                         String ext,
                                         String exts
    ) throws IOException, FileUploadException {
        FileInformation fileInfo = new FileInformation();
        String range = request.getHeader("Range");
        if ((fileSize==null || fileSize==0) || StringUtils.isEmpty(range) || blockIndex==null) {
            fileInfo = this.getLocalFile(file, pathDir, newFileName, ext, exts);
            int flag = fileUploadData.uploadFile(fileInfo);
            Assert.isTrue(flag>0, FileConst.getEnumDesc(flag));
        } else {
            //大文件上传name不能为空
            if (newFileName==null) {
                throw new RuntimeException(FileConst.FILENAME_IS_NULL.toString());
            }
            if (blockIndex==null) {
                blockIndex = 0;
            }
            // 如果文件块为空 则返回info文件的信息
            if (file==null) {
                ProgressInfo progressInfo = getReadProgressInfo(fileSize, newFileName, pathDir);
                fileInfo.setProgressInfo(progressInfo);
                return fileInfo;
            } else {
                fileInfo = this.getLocalFile(file, pathDir, newFileName, ext, exts);
            }

            // 接收文件块并存储
            ProgressInfo progressInfo = receiveFileBlock(file, fileInfo.getFileId(), range, pathDir, fileSize, blockIndex, blockTotal, fileInfo.getExt());
            fileInfo.setProgressInfo(progressInfo);
        }
        return fileInfo;
    }

    private ProgressInfo receiveFileBlock(MultipartFile file,
                                          String fileName,
                                          String range,
                                          String pathDir,
                                          long fileSize,
                                          int blockIndex,
                                          int blockTotal,
                                          String extName) throws IOException {
        // 判断range合法性
        Assert.isTrue(!StringUtils.isEmpty(range), FileConst.FILERANGE_IS_NULL.toString());
        if (!range.startsWith("bytes=")) {
            throw new RuntimeException(FileConst.FILERANGE_FORMAT_ERR.toString());
        }
        range = StringUtils.remove(range, "bytes=");
        String[] split = range.split("-");
        if (split == null || split.length < 2) {
            throw new RuntimeException(FileConst.FILERANGE_FORMAT_ERR.toString());
        }

        long uploadStartByte;
        long uploadEndByte;
        try {
            // 要下载的文件字节数
            uploadStartByte = Long.parseLong(split[0]);
            uploadEndByte = Long.parseLong(split[1]);
        } catch (Exception e) {
            throw new RuntimeException(FileConst.FILERANGE_IS_NOT_NUMBER.toString());
        }
        // 存储临时文件块的文件夹路径
        if (extName==null || (extName!=null && extName.equals("<fdopen>"))) {
            extName = "";
        }

        long fileBlockSizeTmp = (long)Math.ceil((double) fileSize / (double) blockTotal);

        //限制每个分片的大小
        if (fileBlockSizeTmp<this.fileBlockSize || fileBlockSizeTmp<=0) {
            throw new RuntimeException(FileConst.FILE_BLOCK_SIZE_ERR.toString());
        } else {
            this.fileBlockSize = fileBlockSizeTmp;
        }

        String fileBlockProgressFolder = String.format("%s%s%s%s%s#%s#%s", FileUploadUtils.getBasefolderConf(), File.separator, pathDir, File.separator, this.fileBlockFolder, fileName, extName);
        File fileBlockProgressFolderObj = new File(fileBlockProgressFolder);

        if(!fileBlockProgressFolderObj.exists()) {

            //判断路径下是否存在同名的文件名
            boolean isSome = FileUploadUtils.checkExistSomeFile(pathDir, fileName, extName);
            Assert.isTrue(!isSome, FileConst.UPLOAD_FILE_EXISTS_ERR.toString());

            fileBlockProgressFolderObj.mkdirs();
        }
        // 记录part文件进度信息的临时文件
        String infoFile = String.format("%s%s%s.info", fileBlockProgressFolder, File.separator, fileName);
        File infoFileObj = new File(infoFile);
        // 从info文件中读取信息，如果没有则创建
        ProgressInfo info = readInfo(infoFileObj);
        info.setTotalSize(fileSize);

        info.setBlockSize(this.fileBlockSize);
        info.setFileName(fileName);

        long blockStartByte = blockIndex * info.getBlockSize();
        long blockEndByte = Math.min((blockIndex + 1) * info.getBlockSize(), info.getTotalSize());

        long realStartByte = 0;
        long realEndByte = 0;
        if (blockIndex==0) {
            if (uploadStartByte>=info.getBlockSize()) {
                throw new RuntimeException(FileConst.UPLOAD_RANGE_START_ERR.toString());
            } else if (uploadEndByte>info.getBlockSize()) {
                throw new RuntimeException(FileConst.UPLOAD_RANGE_END_ERR.toString());
            } else if (uploadStartByte>uploadEndByte) {
                throw new RuntimeException(FileConst.FILERANGE_FORMAT_ERR.toString());
            } else {
                realStartByte = uploadStartByte;
                realEndByte = uploadEndByte;
            }
        } else {
            //
            if (uploadEndByte<uploadStartByte) {
                throw new RuntimeException(FileConst.FILERANGE_FORMAT_ERR.toString());
            }
            if (uploadStartByte >= blockStartByte && uploadEndByte <= blockEndByte) {
                realStartByte = uploadStartByte - blockStartByte;
                realEndByte = uploadEndByte;
            } else {
                // 块的偏移量错误
                throw new RuntimeException(FileConst.FILERANGE_IS_WRONG.toString());
            }
            if (realEndByte == 0) {
                throw new RuntimeException(FileConst.FILERANGE_IS_WRONG.toString());
            }
        }

        //分片file文件大小和range的字节范围进行判断
        if (file.getSize()!=(uploadEndByte-uploadStartByte)) {
            throw new RuntimeException(FileConst.RANGE_NOT_EQUAL_BLOCKFILESIZE.toString());
        }

        FileChannel fileChannel = null;
        FileLock fileLock = null;
        // 存储文件块的临时文件路径
        String blockPartFile = String.format("%s%s%d.%s", fileBlockProgressFolder, File.separator, blockIndex, this.fileBlockSuffix);
        try {
            File partFile = new File(blockPartFile);
            if (!partFile.exists()) {
                File dirs = new File(partFile.getParent());
                if (!dirs.exists()) {
                    dirs.mkdirs();
                }
                partFile.createNewFile();
            }
            RandomAccessFile accessFile = new RandomAccessFile(partFile, "rw");
            fileChannel = accessFile.getChannel();
            try {
                fileLock = fileChannel.tryLock();
                if (fileLock == null) {
                    // 返回所有的文件块列表信息
                    return readProcess(info, fileBlockProgressFolderObj, blockTotal);
                }
            } catch (Exception e) {
                return readProcess(info, fileBlockProgressFolderObj, blockTotal);
            }
            accessFile.seek(realStartByte);
            InputStream inputStream = file.getInputStream();
            byte[] buffer = new byte[1024];
            int n;
            for (; -1 != (n = inputStream.read(buffer)); ) {
                accessFile.write(buffer, 0, n);
            }
            inputStream.close();
            fileLock.release();
            fileLock.close();
            fileLock = null;
            fileChannel.close();
            fileChannel = null;
            accessFile.close();
        } catch (IOException e) {
            throw new RuntimeException(FileConst.HANDLE_BLOCK_ERR.toString(), e);
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.close();
                    fileLock.release();
                }
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ProgressInfo newProgressInfo = readProcess(info, fileBlockProgressFolderObj, blockTotal);
        List<FileBlockProgress> collect = info.getProcess().stream().filter(p -> !p.isLock() && (p.getEndByte() - p.getStartByte() > p.getUploadedByte())).collect(Collectors.toList());
        writeInfo(newProgressInfo, infoFileObj);
        if (CollectionUtils.isEmpty(collect)) {
            ProgressInfo oldProgressInfo = readInfo(new File(infoFile));
            if (!oldProgressInfo.isFinish()) {
                oldProgressInfo.setFinish(true);
                writeInfo(oldProgressInfo, new File(infoFile));
            }
            String newFilePath = String.format("%s%s%s%s%s", FileUploadUtils.getBasefolderConf(), File.separator, pathDir, File.separator, fileName);
            // 当合并分片的时候 上传路径已经有相同名称的文件 那修改分片合并的文件后面加（Num）
            String extNameTmp = extName.equals("") ? "" : "."+extName;
            File checkFileIsRe = new File(newFilePath+extNameTmp);
            if (checkFileIsRe.exists()) {
                newFilePath = newFilePath+"(1)";
                info.setFileName(fileName+"(1)");
            }
            File mergeFile = FileUploadUtils.mergeFiles(info, fileBlockProgressFolderObj, this.fileBlockSuffix, newFilePath+extNameTmp);
            extNameTmp = null;
            newFilePath = null;
            if (mergeFile.exists())
                info.setFinish(true);
            FileUtils.deleteDirectory(fileBlockProgressFolderObj);
        }
        return info;
    }

    /**
     * 获取进度信息
     * @param fileSize
     * @param fileName
     * @return
     */
    private ProgressInfo getReadProgressInfo(long fileSize, String fileName, String pathDir) throws IOException {
        String fileBlockProgressFolder = String.format("%s%s%s#%s", pathDir, File.separator, this.fileBlockFolder, fileName);
        String fileBlockProgressFolderTmp = FileUploadUtils.createDirectory(fileBlockProgressFolder);
        File infoFile = new File(String.format("%s%s.%s.info", fileBlockProgressFolderTmp, File.separator, fileName));
        ProgressInfo info = null;

        if (!infoFile.exists()) {
            info = createProgressInfo(fileSize, fileName);
            writeInfo(info, infoFile);
        } else {
            info = readInfo(infoFile);
        }
        return info;
    }

    /**
     * 返回文件上传进度信息
     * @param fileName
     * @param pathDir
     * @return
     * @throws IOException
     */
    public Map<String, Long> getReadLocalProgressInfo(String fileName, String pathDir) throws IOException {
        long total = 0;
        long already = 0;
        String extName = "";
        String fileNameNoExt = "";
        int dot = fileName.lastIndexOf('.');
        if ((dot >-1) && (dot < (fileName.length()))) {
            fileNameNoExt = fileName.substring(0, dot);
            extName = fileName.substring(dot+1, fileName.length());
            extName = extName.toLowerCase();
        }

        Map<String, Long> map = new HashMap<>();
        String fileBlockProgressFolder = String.format("%s%s%s#%s", pathDir, File.separator, this.fileBlockFolder, fileNameNoExt);
        if (!extName.equals("")) {
            fileBlockProgressFolder = fileBlockProgressFolder + "#" + extName;
        }

        File fileBlockProgressFolderObj = new File(FileUploadUtils.getDirectoryPath(fileBlockProgressFolder));
        String fileBlockProgressFolderTmp = FileUploadUtils.getDirectoryPath(fileBlockProgressFolder);
        File infoFile = new File(String.format("%s%s%s.info", fileBlockProgressFolderTmp, File.separator, fileNameNoExt));
        if (!infoFile.exists()) {
            String fullDirPath = FileUploadUtils.getDirectoryPath(pathDir);
            String fullFilePath = String.format("%s%s%s", fullDirPath, File.separator, fileName);
            File file = new File(fullFilePath);
            if (file.exists()) {
                total = file.length();
            }
            already = total;
        } else {
            List list = CollectionUtils.arrayToList(fileBlockProgressFolderObj.list());
            Set<String> fileNames = new HashSet<String>(list);

            for (String f:fileNames) {
                String suffix = f.substring(f.lastIndexOf(".") + 1);
                if (!suffix.equals("info")) {
                    File block = new File(String.format("%s%s%s", fileBlockProgressFolderObj.getPath(), File.separatorChar, f));
                    long length = block.length();
                    already += length;
                }
            }
            ProgressInfo progressInfo = readInfo(infoFile);
            total = progressInfo.getTotalSize();
        }
        map.put("total", total);
        map.put("already", already);
        return map;
    }

    /**
     * 获取url下载的文件进度信息
     * @param fileName
     * @param pathDir
     * @return
     */
    public Map<String, Long> getReadURLProgressInfo(String fileName, String pathDir, String url) throws IOException {
        long total = 0;
        long already = 0;
        String tmpUrlPathPartDir = String.format("%s%s_%s", pathDir, File.separator, fileName);
        String dirPath = FileUploadUtils.getDirectoryPath(tmpUrlPathPartDir);
        File dir = new File(dirPath);
        String fullPathDir = FileUploadUtils.getDirectoryPath(pathDir);
        String filePath = String.format("%s%s%s", fullPathDir, File.separator, fileName);
        File file = new File(filePath);
        if (!dir.exists()) {
            // 判断是否下载完
            if (file.exists()) {
                total = file.length();
                already = total;
            }
        } else {
            // 获取临时文件夹下的文件块
            if (file.exists()) {
                already = file.length();
            }
            total = FileUploadUtils.getUrlFileSize(url);
        }
        Map<String, Long> map = new HashMap<>();
        map.put("total", total);
        map.put("already", already);
        return map;
    }

    /**
     * 从临时文件读取文件块的进度信息
     * @param file
     * @return
     */
    private ProgressInfo readInfo(File file) {
        ProgressInfo progressInfo = new ProgressInfo();
        if (!file.exists()) {
            return progressInfo;
        }
        FileInputStream fin = null;
        int ch;
        StringBuffer sb = new StringBuffer();
        try {
            fin = new FileInputStream(file);
            while((ch = fin.read()) != -1) {
                sb.append((char)ch);
            }
            progressInfo = JSONObject.parseObject(sb.toString(), ProgressInfo.class);
            fin.close();
            return progressInfo;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return progressInfo;
    }

    /**
     * 创建文件并写入ProgressInfo对象内容
     * @param progressInfo
     * @param file
     */
    private void writeInfo(ProgressInfo progressInfo, File file) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(JSONObject.toJSONString(progressInfo).getBytes());
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

    /**
     * 初始化ProgressInfo
     * @param fileSize
     * @param fileName
     * @return
     */
    private ProgressInfo createProgressInfo(long fileSize, String fileName) {
        ProgressInfo progressInfo = new ProgressInfo();
        progressInfo.setFinish(false);
        Assert.notNull(fileSize, FileConst.FILESIZE_IS_NULL.toString());
        Assert.notNull(fileName, FileConst.FILENAME_IS_NULL.toString());
        progressInfo.setTotalSize(fileSize);
        progressInfo.setFileName(fileName);
        progressInfo.setBlockSize(this.fileBlockSize);
        return progressInfo;
    }

    private ProgressInfo getAllBlockProcess(ProgressInfo info, File fileBlockProgressFolder, Integer blockTotal) {
        ProgressInfo progressInfo = readProcess(info, fileBlockProgressFolder, blockTotal);
        info.setProcess(progressInfo.getProcess().stream().filter(p -> !p.isLock() && (p.getEndByte() - p.getStartByte() > p.getUploadedByte())).collect(Collectors.toList()));
        return progressInfo;
    }

    /**
     * 获取ProgressInfo内的所有块的信息
     * @param info
     * @param fileBlockProgressFolder
     * @param blockTotal
     * @return
     */
    private ProgressInfo readProcess(ProgressInfo info, File fileBlockProgressFolder, Integer blockTotal) {
        long count = 0;
        if (blockTotal==null || blockTotal==0) {
            Assert.isTrue(info.getBlockSize()!=0, FileConst.FILEBLOCK_SIZE_IS_ZERO.toString());
            count = info.getTotalSize() / info.getBlockSize();
        } else {
            count = blockTotal;
        }
        info.setProcess(new ArrayList<>());
        List list = CollectionUtils.arrayToList(fileBlockProgressFolder.list());
        Set<String> fileNames = new HashSet<String>(list);
        for (long l = 0; l < count; l++) {
            FileBlockProgress fileBlockProgress = new FileBlockProgress();
            String fileName = String.format("%d.%s", l, this.fileBlockSuffix);
            fileBlockProgress.setNumber((int)l);
            fileBlockProgress.setStartByte(l*this.fileBlockSize);
            fileBlockProgress.setEndByte(Math.min((l + 1) * this.fileBlockSize, info.getTotalSize()));
            if (fileNames.contains(fileName)) {
                File block = new File(String.format("%s%s%s", fileBlockProgressFolder.getPath(), File.separatorChar, fileName));
                long length = block.length();
                fileBlockProgress.setUploadedByte(length);
                //判断是否上传完毕
                if (length <= fileBlockProgress.getEndByte() - fileBlockProgress.getStartByte()) {
                    FileOutputStream fileOutputStream = null;
                    FileLock fileLock = null;
                    try {
                        fileOutputStream = new FileOutputStream(block, true);
                        try {
                            fileLock = fileOutputStream.getChannel().tryLock();
                            if (fileLock != null) {
                                fileLock.release();
                            }
                        } catch (Exception e) {
                            // 文件被锁 不显示在返回列表中
                            fileBlockProgress.setLock(true);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fileLock != null) {
                                fileLock.release();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            info.addProcess(fileBlockProgress);
        }
        //统计已经上传的数量
        long uploadSize = info.getProcess().stream().mapToLong(FileBlockProgress::getUploadedByte).sum();
        info.setUploadedSize(uploadSize);
        return info;
    }


    @Override
    public List<Map> readDir(String dir) throws FileUploadException {
        return fileUploadData.readDir(dir);
    }

    @Override
    public boolean deleteFile(String filePath) throws FileUploadException {
        return fileUploadData.deleteFile(filePath);
    }

    @Override
    public String deleteFolder(String dirPath) throws FileUploadException, IOException {
        return fileUploadData.deleteFolder(dirPath);
    }

    @Autowired
    FileConfig fileConfig;

    /**
     * 通过url获取文件信息, 并封装成FileInformation对象
     * @param urlStr
     * @param pathDir
     * @param newFileName
     * @param exts
     * @return
     * @throws IOException
     */
    public URLFileInfomation getUrlFile(String urlStr, String pathDir, String newFileName, String ext, String exts) throws IOException {
        URLFileInfomation urlInfo = FileUploadUtils.urlDownload(urlStr, pathDir, newFileName, true, fileConfig.getDownUrlMaxSize());

        //判断路径下是否存在相同文件
        boolean isSome = FileUploadUtils.checkExistSomeFile(pathDir, newFileName.equals("")?urlInfo.getFileName():newFileName, urlInfo.getExt());
        Assert.isTrue(!isSome, FileConst.UPLOAD_FILE_EXISTS_ERR.toString());

        if (urlInfo.isBigFile==true)
            return urlInfo;

        String extName = urlInfo.getExt();
        if (extName.equals("<fdopen>") || extName.equals("")) {
            if (!ext.equals("")) {
                extName = ext;
                urlInfo.setExt(extName);
            }
        }
        // 判断后缀名
        if (!exts.isEmpty()) {
            Assert.isTrue(FileUploadUtils.checkFileExt(extName, exts), FileConst.EXT_ERR.toString());
        }
        return urlInfo;
    }

    /**
     * 通过本地文件获取文件信息
     * @param file
     * @param pathDir
     * @param newFileName
     * @param exts
     * @return
     * @throws IOException
     */
    public FileInformation getLocalFile(MultipartFile file, String pathDir, String newFileName, String ext, String exts) throws IOException {

        String fileName = file.getOriginalFilename();
        String extName = "";

        if (fileName.lastIndexOf(".")>0){
            extName = fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            if (ext!=null && !ext.equals("")) {
                extName = ext;
            }
        }
        extName = extName.toLowerCase();

        // 判断后缀名
        if (exts!=null && !exts.isEmpty()) {
            Assert.isTrue(FileUploadUtils.checkFileExt(extName, exts), FileConst.EXT_ERR.toString());
        }
        FileInformation fileObj;
        if (newFileName==null) {
            fileObj = new FileInformation(file.getInputStream(), fileName, pathDir, extName);
        } else {
            fileObj = new FileInformation(file.getInputStream(), newFileName, fileName, pathDir, extName);
        }
        fileObj.setFileSize(file.getSize());
        return fileObj;
    }


    public void download(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/x-download");
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            response.setHeader("Content-Disposition", "attachment; filename="+new String(fileName.getBytes("gbk"),"iso-8859-1"));
            File file = FileUploadUtils.getRealFilePath(path);
            if (file==null) {
                throw new RuntimeException(FileConst.FILE_NON_EXISTENT.toString());
            }
            response.setHeader("Content-Length",String.valueOf(file.length()));
            in = new BufferedInputStream(new FileInputStream(file));
            out = new BufferedOutputStream(response.getOutputStream());
            byte[] data = new byte[8192];
            int len = 0;
            int bytesBuffered = 0;
            while (-1 != (len=in.read(data, 0, data.length))) {
                out.write(data, 0, len);
                //优化
                bytesBuffered += len;
                if (bytesBuffered>1024 * 1024) {
                    bytesBuffered = 0;
                    out.flush();
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    public ResponseEntity download(String filePath) throws IOException {
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        File file = FileUploadUtils.getRealFilePath(filePath);
        if(file==null) {
            return new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setPragma("no-cache");
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setExpires(0);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file), headers, HttpStatus.CREATED);
    }


    public String zipFiles(String[] filePaths, String zipName) throws IOException {
        String zipPath = FileUploadUtils.createDirectory(this.zipFolder);
        String zipFile = String.format("%s%s%s.zip", zipPath, File.separator, zipName);
        File zipfile = new File(zipFile);
        if (!zipfile.exists()) {
            zipfile.createNewFile();
        }
        byte[] buf = new byte[1024];
        try {
            Map<String, Integer> map = new HashMap<String, Integer>();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            for (int i=0; i<filePaths.length; i++) {
                File srcfile = FileUploadUtils.getRealFilePath(filePaths[i]);
                if (srcfile!=null) {
                    String tmpName = srcfile.getName();
                    if (!map.containsKey(tmpName)) {
                        map.put(tmpName, 0);
                    } else {
                        int num = map.get(tmpName)+1;
                        map.put(tmpName, num);
                        int dot = tmpName.lastIndexOf('.');
                        if ((dot >-1) && (dot < (tmpName.length()))) {
                            String tmpNameTitle = tmpName.substring(0, dot);
                            String extName = tmpName.substring(dot, tmpName.length());
                            tmpName = tmpNameTitle+"("+ String.valueOf(num)+")"+extName;
                        } else {
                            tmpName = tmpName+"("+ String.valueOf(num)+")";
                        }

                    }
                    FileInputStream in = new FileInputStream(srcfile);
                    out.putNextEntry(new ZipEntry(tmpName));
                    int len;
                    while ((len=in.read(buf))>0) {
                        out.write(buf,0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
            }
            map = null;
            out.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.zipFolder);
        sb.append(File.separator);
        sb.append(String.format("%s.zip", zipName));
        return sb.toString();
    }

}
