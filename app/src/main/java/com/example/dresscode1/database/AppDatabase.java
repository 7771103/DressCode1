package com.example.dresscode1.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.dresscode1.database.dao.CommentDao;
import com.example.dresscode1.database.dao.PostDao;
import com.example.dresscode1.database.entity.CommentEntity;
import com.example.dresscode1.database.entity.PostEntity;

@Database(
    entities = {PostEntity.class, CommentEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    
    public abstract PostDao postDao();
    public abstract CommentDao commentDao();
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "dresscode_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}

