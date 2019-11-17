package me.zonghua.tus.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import me.zonghua.tus.model.FileUtil;
import me.zonghua.tus.model.TusFileUpload;
import me.zonghua.tus.config.AliYunOssConfig;
import me.zonghua.tus.exception.TusException;
import me.zonghua.tus.model.UploadProcessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Service
public class TusFileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(TusFileUploadService.class);

    private final AliYunOssConfig aliYunOssConfig;

    private final RedisTemplate<String, TusFileUpload> redisTemplate;

    public TusFileUploadService(AliYunOssConfig aliYunOssConfig, RedisTemplate<String, TusFileUpload> redisTemplate) {
        this.aliYunOssConfig = aliYunOssConfig;
        this.redisTemplate = redisTemplate;
    }

    public TusFileUpload findOne(String fileId) {
        return redisTemplate.boundValueOps(fileId).get();
    }

    void save(TusFileUpload tusFileUpload) {
        redisTemplate.boundValueOps(tusFileUpload.getFileId()).set(tusFileUpload);
    }

    private void remove(TusFileUpload tusFileUpload) {
        redisTemplate.delete(tusFileUpload.getFileId());
    }

    /**
     * 初始化上传
     *
     * @param fileId       文件Id
     * @param uploadLength 文件大小
     * @return Tus上传记录
     */
    public TusFileUpload initUpload(String fileId, int uploadLength) {
        logger.info("Start init upload, fileId: [{}] size: [{}]", fileId, FileUtil.prettySize(uploadLength));
        OSS ossClient = buildOssClient();
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(aliYunOssConfig.getBucketName(), fileId);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = result.getUploadId();
        logger.info("Init upload id: [{}]", uploadId);

        TusFileUpload tusFileUpload = new TusFileUpload();
        tusFileUpload.setFileId(fileId);
        tusFileUpload.setUploadLength(uploadLength);
        tusFileUpload.setOffset(0);
        tusFileUpload.setUploadId(uploadId);
        tusFileUpload.setWrappedPartETags(new ArrayList<>());
        save(tusFileUpload);
        logger.info("Tus file: [{}] upload save", fileId);
        ossClient.shutdown();
        return tusFileUpload;
    }

    private OSS buildOssClient() {
        return new OSSClientBuilder().build(aliYunOssConfig.getEndpoint(), aliYunOssConfig.getAccessKeyId(), aliYunOssConfig.getAccessKeySecret());
    }

    public PartListing listUploadedParts(TusFileUpload tusFileUpload) {
        ListPartsRequest listPartsRequest = new ListPartsRequest(aliYunOssConfig.getBucketName(), tusFileUpload.getFileId(), tusFileUpload.getUploadId());
        OSS ossClient = buildOssClient();
        PartListing listPartsResult = ossClient.listParts(listPartsRequest);
        ossClient.shutdown();
        return listPartsResult;

    }

    public long uploadPart(TusFileUpload tusFileUpload, InputStream inputStream, Long contentLength, PartListing uploadedPartList) {
        int partNumber = uploadedPartList.getParts().size() + 1;
        // 计算上传的部分流长度
        try {
            if (contentLength == null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                long nread = 0L;
                byte[] buf = new byte[1024];
                int n;
                while ((n = inputStream.read(buf)) > 0) {
                    byteArrayOutputStream.write(buf, 0, n);
                    nread += n;
                }
                inputStream.close();
                inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                contentLength = nread;
            }
        } catch (Exception e) {
            throw new TusException("Read input stream fail", e);
        }
        logger.info("Prepare upload part:[{}] size: [{}]", partNumber, FileUtil.prettySize(contentLength));

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(aliYunOssConfig.getBucketName());
        uploadPartRequest.setKey(tusFileUpload.getFileId());
        uploadPartRequest.setUploadId(tusFileUpload.getUploadId());
        uploadPartRequest.setInputStream(inputStream);
        uploadPartRequest.setPartSize(contentLength);
        uploadPartRequest.setPartNumber(partNumber);
        uploadPartRequest.setProgressListener(new UploadProcessListener(tusFileUpload.getFileId()));
        OSS ossClient = buildOssClient();
        UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);

        // 计算下一次上传的偏移
        int newOffset = 0;
        for (PartSummary part : uploadedPartList.getParts()) {
            newOffset += part.getSize();
        }
        newOffset += uploadPartResult.getPartSize();


        if (newOffset > tusFileUpload.getUploadLength()) {
            throw new TusException("File is bigger than uploadLength");
        }

        tusFileUpload.setOffset(newOffset);
        tusFileUpload.putPartETag(uploadPartResult.getPartETag());
        save(tusFileUpload);
        ossClient.shutdown();
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new TusException("close input steam fail", e);
        }
        logger.info("Uploaded part: [{}] size: [{}]", partNumber, FileUtil.prettySize(contentLength));
        logger.info("New offset: [{}]", newOffset);
        return newOffset;
    }

    public void completeUpload(TusFileUpload tusFileUpload) {
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                aliYunOssConfig.getBucketName(),
                tusFileUpload.getFileId(),
                tusFileUpload.getUploadId(),
                tusFileUpload.retirePartETags()
        );
        OSS ossClient = buildOssClient();
        CompleteMultipartUploadResult completedResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
        logger.info("File: [{}] upload done", tusFileUpload.getFileId());
        remove(tusFileUpload);
        ossClient.shutdown();
    }
}
