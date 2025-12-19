package com.example.dresscode1.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.dresscode1.database.entity.PostEntity;
import java.util.List;

@Dao
public interface PostDao {
    @Query("SELECT * FROM posts WHERE id = :postId")
    LiveData<PostEntity> getPostById(int postId);
    
    @Query("SELECT * FROM posts WHERE id = :postId")
    PostEntity getPostByIdSync(int postId);
    
    @Query("SELECT * FROM posts ORDER BY lastUpdated DESC")
    LiveData<List<PostEntity>> getAllPosts();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPost(PostEntity post);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPosts(List<PostEntity> posts);
    
    @Update
    void updatePost(PostEntity post);
    
    @Query("DELETE FROM posts WHERE id = :postId")
    void deletePost(int postId);
    
    @Query("DELETE FROM posts")
    void deleteAllPosts();
    
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY lastUpdated DESC")
    LiveData<List<PostEntity>> getPostsByUserId(int userId);
    
    @Query("SELECT * FROM posts WHERE city = :city ORDER BY lastUpdated DESC")
    LiveData<List<PostEntity>> getPostsByCity(String city);
    
    @Query("SELECT * FROM posts ORDER BY lastUpdated DESC LIMIT :limit")
    LiveData<List<PostEntity>> getPosts(int limit);
}

