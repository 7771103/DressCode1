package com.example.dresscode1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {
    
    /**
     * 将ISO格式的UTC时间字符串转换为相对时间显示
     * 例如："刚刚"、"2分钟前"、"1小时前"、"2天前"等
     */
    public static String formatRelativeTime(String isoTimeStr) {
        if (isoTimeStr == null || isoTimeStr.isEmpty()) {
            return "";
        }
        
        try {
            // 解析ISO格式时间（UTC时区）
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // 处理可能包含毫秒的情况
            String timeStr = isoTimeStr;
            if (timeStr.contains(".")) {
                timeStr = timeStr.substring(0, timeStr.indexOf("."));
            }
            if (timeStr.contains("+")) {
                timeStr = timeStr.substring(0, timeStr.indexOf("+"));
            }
            if (timeStr.contains("Z")) {
                timeStr = timeStr.replace("Z", "");
            }
            
            Date postTime = isoFormat.parse(timeStr);
            if (postTime == null) {
                return isoTimeStr;
            }
            
            // 获取当前时间
            Date now = new Date();
            
            // 计算时间差（毫秒）
            long diff = now.getTime() - postTime.getTime();
            
            // 转换为秒
            long seconds = diff / 1000;
            
            if (seconds < 60) {
                return "刚刚";
            }
            
            // 转换为分钟
            long minutes = seconds / 60;
            if (minutes < 60) {
                return minutes + "分钟前";
            }
            
            // 转换为小时
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + "小时前";
            }
            
            // 转换为天
            long days = hours / 24;
            if (days < 30) {
                return days + "天前";
            }
            
            // 转换为月
            long months = days / 30;
            if (months < 12) {
                return months + "个月前";
            }
            
            // 转换为年
            long years = months / 12;
            return years + "年前";
            
        } catch (ParseException e) {
            // 如果解析失败，返回原始字符串的简化版本
            try {
                if (isoTimeStr.length() > 16) {
                    return isoTimeStr.substring(5, 16);
                }
                return isoTimeStr;
            } catch (Exception ex) {
                return isoTimeStr;
            }
        }
    }
}

