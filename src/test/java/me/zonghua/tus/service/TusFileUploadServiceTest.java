package me.zonghua.tus.service;

import com.aliyun.oss.model.PartETag;
import me.zonghua.tus.model.TusFileUpload;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class TusFileUploadServiceTest {

    @Autowired
    private TusFileUploadService tusFileUploadService;

    @Test
    public void findOne() {
        TusFileUpload tusFileUpload = new TusFileUpload();
        tusFileUpload.setFileId(UUID.randomUUID().toString());
        tusFileUpload.setWrappedPartETags(new ArrayList<>());
        tusFileUpload.putPartETag(new PartETag(1, "test"));
        tusFileUpload.putPartETag(new PartETag(1, "test"));
        tusFileUpload.putPartETag(new PartETag(1, "test"));
        tusFileUpload.putPartETag(new PartETag(1, "test"));
        tusFileUploadService.save(tusFileUpload);

        TusFileUpload foundTusFileUpload = tusFileUploadService.findOne(tusFileUpload.getFileId());
        Assert.assertNotNull(foundTusFileUpload);
        Assert.assertEquals(tusFileUpload.retirePartETags().get(1).getETag(), foundTusFileUpload.retirePartETags().get(1).getETag());
        Assert.assertEquals(tusFileUpload.retirePartETags().get(2).getPartNumber(), foundTusFileUpload.retirePartETags().get(2).getPartNumber());
        Assert.assertEquals(tusFileUpload.retirePartETags().get(3).getPartCRC(), foundTusFileUpload.retirePartETags().get(3).getPartCRC());
    }

    @Test
    public void save() {
        TusFileUpload tusFileUpload = new TusFileUpload();
        tusFileUpload.setFileId(UUID.randomUUID().toString());
        tusFileUploadService.save(tusFileUpload);
    }
}