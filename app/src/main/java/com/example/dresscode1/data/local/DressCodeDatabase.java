package com.example.dresscode1.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.dresscode1.data.local.dao.UserDao;
import com.example.dresscode1.data.local.entity.UserEntity;

@Database(entities = {UserEntity.class}, version = 2, exportSchema = false)
public abstract class DressCodeDatabase extends RoomDatabase {
    
    private static volatile DressCodeDatabase INSTANCE;
    
    public abstract UserDao userDao();
    
    public static DressCodeDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (DressCodeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            DressCodeDatabase.class,
                            "dresscode_database"
                    )
                    .fallbackToDestructiveMigration() // 开发阶段：模式不匹配时重建数据库
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

