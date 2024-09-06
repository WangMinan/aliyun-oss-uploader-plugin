package edu.npu;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadFileRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @author : [wangminan]
 * @description : 上传工具类
 */
public class AliyunOssUploaderHelper {

    public static boolean upload(String endPoint, String stsUrl, String accessKeyId,
                                 String accessKeySecret, String bucketName, String localPath,
                                 String remotePath, int partSize, int taskNum, TaskListener listener) {
        long SINGLE_PART_SIZE = partSize * 1024 * 1024L;
        String endPointUrl = "https://" + endPoint;
        OSS ossClient;
        if (stsUrl.isEmpty()) {
            ossClient = new OSSClientBuilder().build(
                    endPointUrl, accessKeyId, accessKeySecret);
        } else {
            HttpClient client = HttpClient.newBuilder().build();
            try {
                HttpResponse<String> stsResponse = client.send(HttpRequest.newBuilder().uri(
                        URI.create(stsUrl)).GET().build(), HttpResponse.BodyHandlers.ofString());
                // 解析response 拿到响应体的accessKeyId, accessKeySecret, securityToken
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(stsResponse.body());
                ossClient = new OSSClientBuilder().build(
                        endPointUrl,
                        jsonNode.get("AccessKeyId").asText(),
                        jsonNode.get("AccessKeySecret").asText(),
                        jsonNode.get("SecurityToken").asText());
            } catch (IOException | InterruptedException e) {
                listener.getLogger().println("ERROR: " + e.getMessage());
                return false;
            }
        }
        File uploadFile = new File(localPath);
        if (!uploadFile.exists()) {
            listener.getLogger().println("ERROR: " + Messages.AliyunOssUploaderHelper_ERROR_LocalFileNotFound());
            return false;
        }
        try {
            // 文件小于10M 简单上传 否则分片
            if (uploadFile.length() < 10 * SINGLE_PART_SIZE) {
                listener.getLogger().println(Messages.AliyunOssUploaderHelper_INFO_WillUseSimpleUpload());
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, remotePath, uploadFile);
                ossClient.putObject(putObjectRequest);
            } else {
                listener.getLogger().println(Messages.AliyunOssUploaderHelper_INFO_WillUseMultipartUpload());
                UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, remotePath);
                uploadFileRequest.setUploadFile(localPath);
                uploadFileRequest.setTaskNum(taskNum);
                uploadFileRequest.setPartSize(SINGLE_PART_SIZE);
                uploadFileRequest.setEnableCheckpoint(true);
                uploadFileRequest.setObjectMetadata(new ObjectMetadata());
                ossClient.uploadFile(uploadFileRequest);
            }
            return true;
        } catch (OSSException oe) {
            listener.getLogger().println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            listener.getLogger().println("Error Message:" + oe.getErrorMessage());
            listener.getLogger().println("Error Code:" + oe.getErrorCode());
            listener.getLogger().println("Request ID:" + oe.getRequestId());
            listener.getLogger().println("Host ID:" + oe.getHostId());
            return false;
        } catch (ClientException ce) {
            listener.getLogger().println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            listener.getLogger().println("Error Message:" + ce.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            ossClient.shutdown();
        }
        return false;
    }
}
