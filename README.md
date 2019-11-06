1	获取令牌码	 	/api/v1/auth/token	post	 	
username=admin1；
password=123

{"result":"eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE1MjIwNTA4MDUsInN1YiI6ImFkbWluMSIsImNyZWF0ZWQiOjE1MjIwNTA3NTU3MDB9.PUeXKB1FHzVLR6pW0XbS_2_jevJJfDlibqlAcq8KI0gQ9bT2n1g3zpPr7Q6WR4EAsD0Ex96hevz38jo8dipmOQ","status":"ok"}

2	刷新令牌码	注：此接口和以下所有接口需要在header中带上Authorization：Bearer+空格+token	/api/v1/auth/refreshToken	post	header	在http头文件里填写，Authorization：Bearer+空格+token	{"result":"eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE1MjIwNTE4MDAsInN1YiI6ImFkbWluMSIsImNyZWF0ZWQiOjE1MjIwNTE3NTA3ODh9.Nfxzh9QyvOWdynkQE74ntqyUVkCBUciRe-P5SmQH_oehBWzhLRZQOUyrCn_6Ind85D17vYnkwrgXM_OV_IhTMg","status":"200"}

3	本地文件上传
包括大文件和常规文件的上传，如果是客户端上传大文件需要添加参数
fileSize，blockIndex，blockTotal，不添加这些参数默认为普通文件上传。

确保fileSize是正确，否则blockSize会计算错误，导致分片大小小于blockSize后，文件一直处于待上传状态，或者分片大小大于blockSize，导致头文件的Range参数总会提示错误。

每个分片需要保证大于等于5M，否则没有必要使用分片上传。

 /api/v1/file/localFileUpload

post
multipart/form-data

1.path存储路径， path=/testpath；

2.name自定义文件名称，如果name为空，系统会自动生成唯一字符串代替name。大文件分片上传name不能为空；

 3.file上传的文件，file是File类型 file=test.txt；

7.exts是上传文件允许的后缀名称，在不确定文件是什么类型时可以填写多个类型，逗号分隔；

8.fileSize是文件大小，当要上传客户端认为的大文件时需要填写；

9.blockIndex是大文件分块后的索引，只有在需要上传大文件时会用到；

10.blockTotal是大文件分块后的分块总数，只有上传大文件时才会用到；

11. Range如果时大文件需要在http头文件中输入Range:bytes=开始-结束字节数。遵守http写法的方式。

12. ext是文件的后缀名，如果分片没有后缀，必须要加上这个参数才能告诉系统是什么格式文件。

{"result":{"creationDate":"1523249239901","ext":"exe","fileId":"taobao","fileName":"AliIM2017_taobao.exe","fileSize":67773704,"pathDir":"tmp","progressInfo":{"blockSize":5242880,"fileName":"taobao","finish":false,"process":[{"endByte":10485760,"lock":false,"number":1,"startByte":5242880,"uploadedByte":0},{"endByte":15728640,"lock":false,"number":2,"startByte":10485760,"uploadedByte":0},{"endByte":20971520,"lock":false,"number":3,"startByte":15728640,"uploadedByte":0},{"endByte":26214400,"lock":false,"number":4,"startByte":20971520,"uploadedByte":0}],"totalSize":67773704,"uploadedSize":67773704}},"status":"200"}

fileId:为自定义或者系统定义的新名称；

fileName:原始文件的名称；

pathDir:访问路径；

fileSize:文件类型；

creationDate:上传成功的时间点；

progressInfo:上传文件的分块状态，还剩下多少块没有上传（只有大文件的时候会返回）；

status:200为上传成功。

如果访问的话，自行拼接：

"/tmp/AliIM2017_taobao.exe"


4	远程文件上传	
threadNum控制几个线程进行下载，threadNum或者为0时自动设置为系统默认线程数。

如果远程文件不大于209715200字节就不会使用线程下载。

/api/v1/file/urlFileUpload	post	 	
 

1.path存储路径， path=/testpath；

2.name自定义文件名称，如果name为空，系统会自动生成唯一字符串代替name；

3.exts是上传文件允许的后缀名称，在不确定文件是什么类型时可以填写多个类型，逗号分隔；

4.threadNum是远程文件下载线程数，可以为空；

5.url是远程文件下载地址，如果是客户端本地文件url不需要填写；

 {"result":{"creationDate":"1523861525511","fileName":"jetbrains","fileSize":0,"pathDir":"jet"},"status":"200"}



5	删除文件夹	 	
/api/v1/file/deleteFolder
 post	 	 文件夹路径 folder=/path2，斜线可以省略	 {
"result": "path2",
"status": "OK"
}



6	删除文件	 	
/api/v1/file/deleteFiles
post	 	
可以包括多文件 files[]=/path1/test.txt

{
"result": [
"/path1/test.txt"
],
"status": "OK"
},

删除成功的文件路径会返回，不存在的返回为空。

7	查看文件列表	 	
/api/v1/file/lookupFiles
get	 	文件夹路径 dir=/path1	
{
"result": [
{
"fileName": "test-1.txt",
"creationTime": "2018-03-08T07:47:20.000554Z",
"fileSize": 1624,
"isRegularFile": true,
"isDirectory": false
},
{
"fileName": "test-2.txt",
"creationTime": "2018-03-08T07:47:20.00056Z",
"fileSize": 2632,
"isRegularFile": true,
"isDirectory": false
}
],
"status": "OK"
}

8	下载文件	 	
/api/v1/file/download
get	 	filePath：文件路径 filePath=/path1/test.txt	返回文件流
9	压缩下载多文件	
当不同路径下的相同名称的文件一起压缩下载的时候，会更改其中一个文件为（数字），比如：text(1).txt。

相同路径的相同文件会选择一个文件下载。

/api/v1/file/downloadZip
post	 	
filePath[]：多文件路径 ，

zipName：压缩文件的名称，不需要写后缀名

返回zip压缩文件
10	
在线查看图片
 	
 
/api/v1/file/img
get	 	path：查看路径	返回图片二进制流
11	查看大文件上传进度信息	 	
/api/v1/file/processStatus
post	 	
name：正在上传的文件名称；

path：在上传的文件路径；

url：如果是远程大文件，则输入此参数；

isLocalFlag：1为本地文件，0为远程文件

{"result":{"total":0,"already":0},"status":"200"}

total：文件总大小；

already：已经上传了多少字节


附带java代码的sdk，可以使用，也可以自己编写api调用，对以上接口请求操作的封装，仅依赖com.alibaba.fastjson包，jar内不包含，需要额外导入。

例如：  

大文件分片上传使用

import com.vega.sdk.file.FileManager;

import com.vega.sdk.file.SplitFileManager;

//文件分片上传接口调用

SplitFileManager splitFileManager = new SplitFileManager("localhost:8080", "admin1", "123");   //参数1是服务器地址和端口号，为空默认是localhost:8080；参数2是用户名， 参数3是密码；

splitFileManager.splitUploadFile("C:/Users/vega/Desktop/1.txt", "vega-test-11", 7)； //splitUploadFile方法参数1：文件路径，参数2：路径名称，参数3：几个分片；

 

FileManager fm = new FileManager("localhost:8080", "admin1", "123");

//下载文件接口，只有deleteFiles， downloadZip的参数会使用IdentityHashMap，其他接口都是HashMap

IdentityHashMap map = new IdentityHashMap();

map.put("zipName", "222");

map.put("filePath[]", "vega-test-11/111.png");

fm.downloadZip(map);     //返回文件流

//普通上传文件接口

Map map = new HashMap();

map.put("name", "222");

map.put("path", "vega-test-222");

map.put("ext", "txt");

fm.localFileUpload(map, "C:/Users/vega/Desktop/3333.rar")；

fileOperate is a GPL-licensed suite of program.
my e-mail：511748821@qq.com
