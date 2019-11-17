package me.zonghua.tus.model;

public class WrappedPartETag  {

    private int partNumber;
    private String eTag;
    private long partSize;
    private Long partCRC;

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public Long getPartCRC() {
        return partCRC;
    }

    public void setPartCRC(Long partCRC) {
        this.partCRC = partCRC;
    }
}
