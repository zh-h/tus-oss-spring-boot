package me.zonghua.tus.model;

import com.aliyun.oss.model.PartETag;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import me.zonghua.tus.model.WrappedPartETag;

import java.util.List;
import java.util.stream.Collectors;

public class TusFileUpload {
    private String fileId;
    private String uploadId;
    private long offset;
    private long uploadLength;
    private List<WrappedPartETag> wrappedPartETags;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }


    public long getUploadLength() {
        return uploadLength;
    }

    public void setUploadLength(long uploadLength) {
        this.uploadLength = uploadLength;
    }

    public List<WrappedPartETag> getWrappedPartETags() {
        return wrappedPartETags;
    }

    public void setWrappedPartETags(List<WrappedPartETag> wrappedPartETags) {
        this.wrappedPartETags = wrappedPartETags;
    }

    @JsonIgnoreProperties
    public List<PartETag> retirePartETags() {
        return this.wrappedPartETags.stream().map(it -> {
            return new PartETag(it.getPartNumber(), it.geteTag(), it.getPartSize(), it.getPartCRC());
        }).collect(Collectors.toList());
    }

    @JsonIgnoreProperties
    public void putPartETag(PartETag partETag) {
        WrappedPartETag wrappedPartETag = new WrappedPartETag();
        wrappedPartETag.setPartNumber(partETag.getPartNumber());
        wrappedPartETag.seteTag(partETag.getETag());
        wrappedPartETag.setPartSize(partETag.getPartSize());
        wrappedPartETag.setPartCRC(partETag.getPartCRC());
        wrappedPartETags.add(wrappedPartETag);
    }

}
