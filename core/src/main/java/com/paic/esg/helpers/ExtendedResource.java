package com.paic.esg.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ExtendedResource {

    private InputStream inputStream = null;
    private boolean localResource;

    public InputStream getAsStream() {
        return inputStream;
    }

    public ExtendedResource(String name) {
        this(name, System.getProperty("mainConfig.path"));
        //this(name, System.getProperty("user.dir"));
    }

    public ExtendedResource(String name, String userDirectory) {
        try {
            String externalFile = userDirectory + "/" + name;
            File file = new File(externalFile);
            if (file.exists()) {
                inputStream = new FileInputStream(file);

            } else {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(name);
                localResource = true;
            }
        } catch (Exception e) {
            // log error
        }
    }
    public Boolean isLocalResource(){
        return this.localResource;
    }

}
