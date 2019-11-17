package me.zonghua.tus.model;

import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadProcessListener implements ProgressListener {

    private static final Logger logger = LoggerFactory.getLogger(UploadProcessListener.class);

    private long size;

    private long process;

    private String fileId;

    public UploadProcessListener(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {

        switch (progressEvent.getEventType()) {
            case TRANSFER_PART_STARTED_EVENT: {
                size = 0L;
                process = 0L;
                logger.info("Start file: [{}] upload", fileId);
            }
            break;
            case REQUEST_CONTENT_LENGTH_EVENT: {
                size = progressEvent.getBytes();
            }
            break;
            case REQUEST_BYTE_TRANSFER_EVENT: {
                process += progressEvent.getBytes();
            }
            case TRANSFER_PREPARING_EVENT: {

            }
            break;
            case TRANSFER_PART_COMPLETED_EVENT: {
                if (process == size) {
                    logger.info("File: [{}] uploaded: [{}%] size: [{}]", fileId, size == 0 ? 0 : ((process / size) * 100D), FileUtil.prettySize(process));
                    logger.info("File: [{}] completed upload", fileId);
                }
            }
            ;
            break;
            default:
                break;
        }
    }
}