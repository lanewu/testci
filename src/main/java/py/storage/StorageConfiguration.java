package py.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource({ "classpath:config/storage.properties" })
public class StorageConfiguration {
    @Value("${segment.size.byte:1073741824}")
    private long segmentSizeByte = 1073741824l;

    @Value("${page.size.byte:8192}")
    private long pageDataSizeByte = 8192;

    @Value("${io.timeout.ms:120000}")
    private int ioTimeoutMS;

    @Value("${fs.block.size.byte:819200}")
    private long fsBlockSizeByte = 819200l;

    @Value("${enventdata.output.rootpath:/var/testing}")
    private String outputRootpath = "/var/testing";

    public long getSegmentSizeByte() {
        return segmentSizeByte;
    }

    public void setSegmentSizeByte(long segmentSizeByte) {
        this.segmentSizeByte = segmentSizeByte;
    }

    public long getPageSizeByte() {
        return pageDataSizeByte;
    }

    public void setPageSizeByte(long pageDataSizeByte) {
        this.pageDataSizeByte = pageDataSizeByte;
    }

    public void setIOTimeoutMS(int ioTimeoutMS) {
        this.ioTimeoutMS = ioTimeoutMS;
    }

    public int getIOTimeoutMS() {
        return ioTimeoutMS;
    }

    public String getOutputRootpath() {
        return outputRootpath;
    }

    public void setOutputRootpath(String outputRootpath) {
        this.outputRootpath = outputRootpath;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public long getFsBlockSizeByte() {
        return fsBlockSizeByte;
    }

    public void setFsBlockSizeByte(long fsBlockSizeByte) {
        this.fsBlockSizeByte = fsBlockSizeByte;
    }

    @Override
    public String toString() {
        return "StorageConfiguration [segmentSizeByte=" + segmentSizeByte + ", pageDataSizeByte=" + pageDataSizeByte
                + ", ioTimeoutMS=" + ioTimeoutMS + ", fsBlockSizeByte=" + fsBlockSizeByte + "]";
    }

    @Bean
    public EdRootpathSingleton rootpathSingleton() {
        EdRootpathSingleton edRootpathSingleton = EdRootpathSingleton.getInstance();
        edRootpathSingleton.setRootPath(outputRootpath);
        return edRootpathSingleton;
    }

    }
