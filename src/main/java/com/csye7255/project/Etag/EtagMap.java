package com.csye7255.project.Etag;

import java.util.HashMap;
import java.util.Map;

public class EtagMap {
    private static Map<String,String> etags;

    private EtagMap(){}

    public static Map<String,String> getEtags(){
        if(etags == null){
            etags = new HashMap<>();
        }
        return etags;
    }

    public static void setEtags(Map<String, String> etags) {
        EtagMap.etags = etags;
    }
}
