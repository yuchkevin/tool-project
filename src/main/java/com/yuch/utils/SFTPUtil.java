package com.yuch.utils;

import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

/**
 * 类说明 sftp工具类
 */
public class SFTPUtil {
    private transient Logger log = LoggerFactory.getLogger(this.getClass());

    private ChannelSftp sftp;

    private Session session;
    /**
     * SFTP 登录用户名
     */
    private String username;
    /**
     * SFTP 登录密码
     */
    private String password;
    /**
     * 私钥
     */
    private String privateKey;
    /**
     * SFTP 服务器地址IP地址
     */
    private String host;
    /**
     * SFTP 端口
     */
    private int port;


    /**
     * 构造基于密码认证的sftp对象
     */
    public SFTPUtil(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    /**
     * 构造基于秘钥认证的sftp对象
     */
    public SFTPUtil(String username, String host, int port, String privateKey) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
    }

    public SFTPUtil() {
    }


    /**
     * 初始化ftp服务器
     */
    public void login() throws Exception {
        try {
            JSch jsch = new JSch();
            if (privateKey != null) {
                jsch.addIdentity(privateKey);
            }
            session = jsch.getSession(username, host, port);
            if (password != null) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
        } catch (Exception e) {
            log.error("sftp异常：{}", e);
            throw new Exception("初始化ftp服务器出错：" + e.getMessage());
        }
    }

    /**
     * 关闭connection
     */
    public void logout() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }


    /**
     * 将输入流的数据上传到sftp作为文件。文件完整路径=basePath+directory
     *
     * @param pathname       上传到该目录
     * @param fileName       sftp端文件名
     * @param originfilename 要上传的本地文件
     */
    public void upload(String pathname, String fileName, String originfilename) throws Exception {
        InputStream inputStream = new FileInputStream(new File(originfilename));
        upload(pathname, fileName, inputStream);
    }

    public OutputStream upload(String pathname) throws Exception {
        return sftp.put(pathname);
    }

    /**
     * @param pathname 原始文件路径
     * @param fileName 文件名称
     * @param in       文件输入流
     * @return void
     * @author LiuXin
     * @e-mail allen.x.liu@outlook.com
     * @date 2020/4/7 16:39
     * @description SFTP文件上传
     * @method upload
     */
    public void upload(String pathname, String fileName, InputStream in) throws Exception {
        // 判断SFTP当前目录是否存在
        if (isDirExist(pathname)) {
            sftp.cd(pathname);
        } else {
            // 建立目录
            sftp.mkdir(pathname);
            // 进入并设置为当前目录
            sftp.cd(pathname);
        }
        // 进行文件上传
        try (InputStream inputStream = in) {
            sftp.put(inputStream, fileName);
            Properties properties = System.getProperties();
            String os = properties.getProperty("os.name").toLowerCase();
            if (!os.contains("windows")) {
                sftp.cd("../../");
            }
        } catch (SftpException e) {
            log.error("sftp异常：{}", e);
            throw new Exception("上传文件出错：" + e.getMessage());
        }

    }


    /**
     * 下载文件。
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     */
    public void download(String directory, String downloadFile, String saveFile) throws Exception {
        try (OutputStream outputStream = new FileOutputStream(new File(saveFile))) {
            if (directory != null && !"".equals(directory)) {
                sftp.cd(directory);
            }
            sftp.get(downloadFile, outputStream);
            outputStream.close();
            Properties properties = System.getProperties();
            String os = properties.getProperty("os.name").toLowerCase();
            if (!os.contains("windows")) {
                sftp.cd("../../");
            }

        } catch (Exception e) {
            log.error("sftp异常：{}", e);
            throw new Exception("下载文件出错：" + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件名
     * @return 字节数组
     */
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        InputStream is = sftp.get(downloadFile);

        byte[] fileData = IOUtils.toByteArray(is);


        return fileData;
    }


    /**
     * 删除文件
     *
     * @param directory  要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public void delete(String directory, String deleteFile) throws Exception {
        try {
            sftp.cd(directory);
            sftp.rm(deleteFile);
            Properties properties = System.getProperties();
            String os = properties.getProperty("os.name").toLowerCase();
            if (!os.contains("windows")) {
                sftp.cd("../../");
            }
        } catch (Exception e) {
            log.error("sftp异常：{}", e);
            throw new Exception("删除文件出错：" + e.getMessage());
        }
    }


    /**
     * 列出目录下的文件
     *
     * @param directory 要列出的目录
     */
    public Vector<?> listFiles(String directory) throws Exception {
        try {
            return sftp.ls(directory);
        } catch (Exception e) {
            log.error("sftp异常：{}", e);
            throw new Exception("获取指定路径下的文件出错：" + e.getMessage());
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath
     * @return
     */
    public boolean exists(String filePath) {
        try {
            sftp.stat(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean mkdir(String filePath) {
        try {
            sftp.mkdir(filePath);
            return true;
        } catch (SftpException e) {
            log.error("sftp异常：{}", e);
            return false;
        }
    }

    /**
     * @param directory 文件目录
     * @return boolean
     * @author LiuXin
     * @e-mail allen.x.liu@outlook.com
     * @date 2020/4/7 18:32
     * @description 判断SFTP上是否存在相应目录
     * @method isDirExist
     */
    public boolean isDirExist(String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = sftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }

}
