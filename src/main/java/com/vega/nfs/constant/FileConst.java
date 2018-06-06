package com.vega.nfs.constant;

public enum FileConst {
    FILE_BLOCK_SIZE_ERR(-27, "The file block size error!"),
    INPUT_NOT_NUM(-26, "Please input the number!"),
    RANGE_NOT_EQUAL_BLOCKFILESIZE(-25, "The file size and range are not equal!"),
    UPLOAD_RANGE_END_ERR(-24, "Calculate the range end location failure!"),
    UPLOAD_RANGE_START_ERR(-23, "Calculate the range start location failure!"),
    FILE_NON_EXISTENT(-22, "The file does not exist!"),
    NOT_IMG_ERR(-21, "Not pictures!"),
    URL_PART_MISSING_ERR(-20, "File missing information!"),
    URL_THREAD_COUNT_ERR(-19, "The number of threads is not appropriate!"),
    URL_DOWNLOAD_ERR(-18, "Multi-threaded download failed!"),
    FILE_PATH_ERR(-17, "Illegal path!"),
    HANDLE_BLOCK_ERR(-16, "Error handling file block!"),
    FILEBLOCK_SIZE_IS_ZERO(-15, "File block size is zero!"),
    FILERANGE_IS_WRONG(-14, "Calculate the range location failure!"),
    FILERANGE_IS_NOT_NUMBER(-13, "The range position is incorrect!"),
    FILERANGE_FORMAT_ERR(-12, "The range location format is incorrect!"),
    FILERANGE_IS_NULL(-11, "No range location is specified!"),
    FILENAME_IS_NULL(-10, "The file name cannot be empty!"),
    FILESIZE_IS_NULL(-9, "File length cannot be empty!"),
    CLOSE_FILE_ERR(-8, "Failed to close file!"),
    UPLOAD_FILE_NULL_ERR(-7, "File is null!"),
    UPLOAD_FILE_EXISTS_ERR(-6, "File already exists in the current folder!"), //上传文件已经存在
    EXT_ERR(-5, "File suffix name error"),            //文件后缀名错误
    DEL_DIR_ERR(-4, "Delete directory failed"),        //删除文件夹失败
    DEL_BASEDIR_ERR(-3, "The home directory cannot be deleted"),    //不能删除主目录
    DEL_NOTDIR_ERR(-2, "Folder does not exist"),     //删除的不是文件夹
    DEL_DIR_EXIST_ERR(-1, "The folder contains files that cannot be deleted"), //删除有文件的文件夹
    UPLOAD_FILE_ERR(0, "Failed to upload file"),       //上传文件失败
    UPLODA_FILE_SUCCESS(1, "Upload file success"),
    UPLOAD_FILE_BIGSIZE(2, "Upload file big size"),
    URL_DOWNLOAD_SUCCESS(3, "Multi-threaded download success!");
    
    private int code;
    private String desc = "";

    private FileConst(int code) {
        this.code = code;
    }

    FileConst(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return desc;
    }

    public static String getEnumDesc(int code) {
        for (FileConst f : FileConst.values()) {
            if (f.getCode()==code) {
                return f.toString();
            }
        }
        return null;
    }

}
