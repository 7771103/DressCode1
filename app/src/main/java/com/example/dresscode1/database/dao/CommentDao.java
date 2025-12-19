package com.example.dresscode1.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.dresscode1.database.entity.CommentEntity;
import java.util.List;

@Dao
public interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt DESC")
    LiveData<List<CommentEntity>> getCommentsByPostId(int postId);
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt DESC")
    List<CommentEntity> getCommentsByPostIdSync(int postId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(CommentEntity comment);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComments(List<CommentEntity> comments);
    
    @Query("DELETE FROM comments WHERE postId = :postId")
    void deleteCommentsByPostId(int postId);
    
    @Query("DELETE FROM comments")
    void deleteAllComments();
}

