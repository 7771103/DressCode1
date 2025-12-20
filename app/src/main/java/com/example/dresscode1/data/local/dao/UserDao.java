package com.example.dresscode1.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.dresscode1.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);
    
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    LiveData<UserEntity> getUserByPhone(String phone);
    
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    UserEntity getUserByPhoneSync(String phone);
    
    @Query("DELETE FROM users")
    void deleteAllUsers();
}

