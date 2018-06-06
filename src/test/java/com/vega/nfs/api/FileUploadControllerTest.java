package com.vega.nfs.api;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;

public class FileUploadControllerTest {


//    @Autowired
//    private FileUploadUtils util;

    @Test
    public void testGetDocuments() throws IOException {
        Path file = Paths.get("D:\\workspace\\gitspace\\spring-boot-upload-file\\path1\\5a3c1c07-b496-4cf5-bcfd-c14fa51d5098.java");
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file,UserDefinedFileAttributeView.class);
        String name = "user.filename";
        ByteBuffer buf = ByteBuffer.allocate(view.size(name));
        view.read(name, buf);
        buf.flip();
        String value = Charset.defaultCharset().decode(buf).toString();
        System.out.println(value);
    }

    @Test
    public void testSetDoc() throws IOException {
        Path path = Paths.get("D:\\workspace\\gitspace\\spring-boot-upload-file\\path1\\db8aecc2-4a29-4a7d-80e2-5f3a6a42574a");
        UserDefinedFileAttributeView view =  Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        view.write("user.filename", ByteBuffer.wrap("sssssss".getBytes()));
    }

}
