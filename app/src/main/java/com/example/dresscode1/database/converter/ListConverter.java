package com.example.dresscode1.database.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListConverter {
    private static Gson gson = new Gson();

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toList(String data) {
        if (data == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(data, listType);
    }
}

