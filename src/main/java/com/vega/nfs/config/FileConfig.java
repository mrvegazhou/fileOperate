package com.vega.nfs.config;

import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nfs")
public class FileConfig {
    private String basefolder;
    public String getBasefolder() {
        return basefolder;
    }
    public void setBasefolder(String basefolder) {
        this.basefolder = basefolder;
    }

    private int downUrlTimeOut;
    public int getDownUrlTimeOut() { return downUrlTimeOut; };
    public void setDownUrlTimeOut(int downUrlTimeOut) { this.downUrlTimeOut = downUrlTimeOut; };

    private int downUrlMaxSize;
    public int getDownUrlMaxSize() { return downUrlMaxSize; };
    public void setDownUrlMaxSize(int downUrlMaxSize) { this.downUrlMaxSize = downUrlMaxSize; }

}
