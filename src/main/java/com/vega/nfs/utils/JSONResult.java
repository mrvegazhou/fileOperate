package com.vega.nfs.utils;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JSONResult{
    public static String reResultString(String status, String message, Object result){
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("status", status);
        jsonMap.put("message", message);
        jsonMap.put("result", result);
        return JSONObject.toJSONString(jsonMap);
    }
}
