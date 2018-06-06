package com.vega.nfs;

import com.vega.nfs.constant.FileConst;
import com.vega.nfs.utils.FileUploadUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(SpringRunner.class)
@ContextConfiguration
@SpringBootTest
@WebAppConfiguration
public class NfsApplicationTests {

	@Test
	public void contextLoads() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		System.out.println(encoder.encode(""));
	}

	@Test
	public void filesize() throws IOException {
//		String aaa = "ss/ss\\s  ";
//		System.out.println(aaa.indexOf("\\"));
//	    File ff = new File("D:\\workspace\\gitspace\\spring-boot-upload-file\\longxin\\hhhh\\_jetbrains");
//		System.out.println(ff.delete());
//		File filea = new File("C:\\Users\\vega\\Desktop\\3333.rar");
//		long a = filea.length();
//		System.out.println(Math.ceil(5024703.04));
		File fileb = new File("C:\\Users\\vega\\Downloads\\part01");
		long b = fileb.length();
		System.out.println(b);
//		System.out.println(b);
//		System.out.println(a+b);

//		File file = new File("D:\\workspace\\tmp\\text.info");
//		RandomAccessFile raf = new RandomAccessFile(file, "rwd");
//		raf.setLength(50000);
//		raf.close();

	}

	@Test
	public void testString() throws IOException {
		FileOutputStream output = null;
		try {
			File file = new File("D:\\workspace\\gitspace\\spring-boot-upload-file\\longxin\\temp\\lump\\af6230189595fce677bf057858e889ca\\test.txt");
			output = new FileOutputStream(file);
			IOUtils.write("ssssss", output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (output != null) {
				output.close();
			}
		}
	}

	@Test
	public void connTest()  {
		try {
			URL url = new URL("https://avatar.csdn.net/4/6/6/1_q649381130.jpg?1522326253");
			//创建连接对象,此时未建立连接
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//设置请求方式为get请求
			conn.setRequestMethod("GET");
			//设置连接超时
			conn.setConnectTimeout(5000);
			//设置读取超时
			conn.setReadTimeout(5000);
			conn.setRequestProperty("Range", "bytes=0-20");
			System.out.println(conn.getResponseCode());

			//设置本次http请求所请求的数据的区间
			//conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}




