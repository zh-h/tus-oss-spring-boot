package me.zonghua.tus.controller;

import com.aliyun.oss.model.PartListing;
import me.zonghua.tus.exception.TusException;
import me.zonghua.tus.model.TusFileUpload;
import me.zonghua.tus.service.TusFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/tus")
public class TusController {

    private final TusFileUploadService tusFileUploadService;

    private static final long MAX_SIZE = 1024 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(TusController.class);

    public TusController(TusFileUploadService tusFileUploadService) {
        this.tusFileUploadService = tusFileUploadService;
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    void getTusOptions(HttpServletResponse response) throws Exception {

        logger.info("Get tus options");

        response.setHeader("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension");
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Tus-Version", "1.0.0,0.2.2,0.2.1");
        response.setHeader("Tus-Max-Size", String.valueOf(1024 * 1024 * 1024));
        response.setHeader("Tus-Extension", "creation,expiration");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setStatus(204);
    }


    @RequestMapping(method = RequestMethod.POST)
    void processPost(@RequestHeader("Upload-Length") Integer uploadLength,
                     @RequestHeader("Upload-FileId") String fileId,
                     HttpServletRequest request,
                     HttpServletResponse response) throws Exception {

        if (uploadLength < 1) {
            throw new RuntimeException("Wrong Final-Length Header");
        }
        if (uploadLength > MAX_SIZE) {
            throw new RuntimeException("wrong Final-Length Header, max is: " + MAX_SIZE);
        }
        tusFileUploadService.initUpload(fileId, uploadLength);
        response.setHeader("Access-Control-Expose-Headers", "Location, Tus-Resumable");
        String location = UriComponentsBuilder.fromUriString(request.getRequestURI() + "/" + fileId).build().toString();
        logger.info("Upload location: [{}]", location);
        response.setHeader("Location", location);
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setStatus(201);
    }

    @RequestMapping(method = RequestMethod.HEAD, value = "/{fileId}")
    void processHead(@PathVariable String fileId, HttpServletResponse response) throws Exception {

        TusFileUpload file = tusFileUploadService.findOne(fileId);
        if (file == null) {
            String message = String.format("File: [%s] not found", fileId);
            logger.warn(message);
            throw new RuntimeException(fileId);
        }
        logger.info("File: [{}] upload offset: [{}]", fileId, file.getOffset());
        response.setHeader("Access-Control-Expose-Headers", "Upload-Offset, Upload-Length, Tus-Resumable");
        response.setHeader("Upload-Offset", Long.toString(file.getOffset()));
        response.setHeader("Upload-Length", Long.toString(file.getUploadLength()));
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setStatus(200);
    }


    @RequestMapping(method = {RequestMethod.PATCH, RequestMethod.POST}, value = "/{fileId}")
    void processPatch(@RequestHeader("Upload-Offset") Long uploadOffset,
                      @RequestHeader(value = "Content-Length", required = false) Long contentLength,
                      @RequestHeader("Content-Type") String contentType,
                      @PathVariable String fileId,
                      InputStream inputStream,
                      HttpServletResponse response) throws Exception {
        if (uploadOffset == null || uploadOffset < 0) {
            throw new TusException("Wrong Offset Header");
        }

        if (!contentType.equals("application/offset+octet-stream")) {
            throw new TusException("Wrong Content-Type Header");
        }

        TusFileUpload tusFileUpload = tusFileUploadService.findOne(fileId);
        if (tusFileUpload == null) {
            String message = String.format("File %s not found", fileId);
            logger.warn(message);
            throw new TusException(message);
        }

        logger.info("Tus file offset: [{}]", tusFileUpload.getOffset());
        logger.info("Tus file final length: [{}]", tusFileUpload.getUploadLength());

        if (tusFileUpload.getUploadLength() < tusFileUpload.getOffset()) {
            throw new TusException("Wrong upload length.");
        }


        //successful
        if (tusFileUpload.getUploadLength() == tusFileUpload.getOffset()) {
            tusFileUploadService.completeUpload(tusFileUpload);
            response.setHeader("Upload-Offset", Long.toString(tusFileUpload.getOffset()));
            response.setStatus(200);
            return;
        }


        PartListing uploadedParts = tusFileUploadService.listUploadedParts(tusFileUpload);


        // 真正上传
        long newOffset = tusFileUploadService.uploadPart(tusFileUpload, inputStream, contentLength, uploadedParts);
        // 不需要这次上传完成就通知200状态，到下次 patch 获取状态，否则 python 客户端会验证失败。
        if (tusFileUpload.getUploadLength() == newOffset) {
            tusFileUploadService.completeUpload(tusFileUpload);
        }


        response.setHeader("Access-Control-Expose-Headers", "Upload-Offset, Tus-Resumable");
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Upload-Offset", Long.toString(newOffset));
        response.setStatus(204);
    }

}
