package me.zonghua.tus.controller;

import io.tus.java.client.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@RunWith(SpringRunner.class)
public class TusControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(TusControllerTest.class);

    @Value("${server.port:8080}")
    private int serverPort;

    private String uploadUrl = null;

    @Before
    public void setUp() throws Exception {
        uploadUrl = String.format("http://localhost:%d/tus", serverPort);
    }

    @Test
    public void testUpload() throws MalformedURLException, FileNotFoundException {
        TusClient client = new TusClient();

        client.setUploadCreationURL(new URL(uploadUrl));
        client.enableResuming(new TusURLMemoryStore());

        String uploadFile = "/Users/x/Downloads/iTerm2-3_3_6.zip";
        String fileId = "test.txt";

        File file = new File(uploadFile);
        Map<String, String> headers = new HashMap<>();
        headers.put("Upload-FileId", "test.txt");
        client.setHeaders(headers);
        final TusUpload upload = new TusUpload(file);
        upload.setFingerprint(uploadFile);
        logger.info("Starting upload...");

        TusExecutor executor = new TusExecutor() {
            @Override
            protected void makeAttempt() throws ProtocolException, IOException {
                TusUploader uploader = client.resumeOrCreateUpload(upload);
                // 2M
                uploader.setChunkSize(2 * 1024 * 1024);
                do {
                    long totalBytes = upload.getSize();
                    long bytesUploaded = uploader.getOffset();
                    double progress = (double) bytesUploaded / totalBytes * 100;

                    String processMessage = String.format("Upload at %06.2f%%", progress);
                    logger.info(processMessage);
                } while (uploader.uploadChunk() > -1);
                uploader.finish();

                logger.info("Upload finished.");
                logger.info(String.format("Upload available at: %s", uploader.getUploadURL().toString()));
            }
        };
        try {
            boolean result = executor.makeAttempts();
            Assert.assertTrue(result);
        } catch (ProtocolException | IOException e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }
    }
}