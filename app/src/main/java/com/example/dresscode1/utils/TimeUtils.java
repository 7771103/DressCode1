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
            Date postTime = null;
            
            // 尝试多种时间格式解析
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",  // 带微秒
                "yyyy-MM-dd'T'HH:mm:ss.SSS",     // 带毫秒
                "yyyy-MM-dd'T'HH:mm:ss",         // 标准格式
                "yyyy-MM-dd HH:mm:ss"            // 空格分隔
            };
            
            String timeStr = isoTimeStr.trim();
            // 处理时区标识
            boolean isUTC = timeStr.contains("Z") || timeStr.endsWith("+00:00");
            if (timeStr.contains("+")) {
                timeStr = timeStr.substring(0, timeStr.indexOf("+"));
            }
            if (timeStr.contains("Z")) {
                timeStr = timeStr.replace("Z", "");
            }
            // 处理毫秒部分
            if (timeStr.contains(".")) {
                int dotIndex = timeStr.indexOf(".");
                String beforeDot = timeStr.substring(0, dotIndex);
                String afterDot = timeStr.substring(dotIndex + 1);
                // 保留最多6位小数
                if (afterDot.length() > 6) {
                    afterDot = afterDot.substring(0, 6);
                }
                timeStr = beforeDot + "." + afterDot;
            }
            
            // 尝试解析为UTC时间
            if (isUTC) {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    postTime = isoFormat.parse(timeStr);
                } catch (ParseException e) {
                    // 继续尝试其他格式
                }
            }
            
            // 如果UTC解析失败，尝试本地时间解析
            if (postTime == null) {
                for (String format : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                        // 先尝试本地时区
                        postTime = sdf.parse(timeStr);
                        if (postTime != null) {
                            break;
                        }
                    } catch (ParseException e) {
                        // 继续尝试下一个格式
                    }
                }
            }
            
            if (postTime == null) {
                // 解析失败，返回日期部分
                if (timeStr.length() >= 10) {
                    return timeStr.substring(0, 10);
                }
                return timeStr;
            }
            
            // 获取当前时间
            Date now = new Date();
            
            // 计算时间差（毫秒）
            long diff = now.getTime() - postTime.getTime();
            
            // 处理负数时间差（未来时间）
            if (diff < 0) {
                // 如果时间差是负数，说明是未来时间，显示为"刚刚"或返回日期
                if (Math.abs(diff) < 60000) { // 1分钟内
                    return "刚刚";
                }
                // 否则返回日期
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                return dateFormat.format(postTime);
            }
            
            // 转换为秒
            long seconds = diff / 1000;
            
            // 如果时间差小于60秒，显示"刚刚"
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
            
        } catch (Exception e) {
            // 如果所有解析都失败，返回日期部分或原始字符串
            try {
                if (isoTimeStr.length() >= 10) {
                    return isoTimeStr.substring(0, 10);
                }
                return isoTimeStr;
            } catch (Exception ex) {
                return "";
            }
        }
    }
}


