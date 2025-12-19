package com.example.dresscode1.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.repository.PostRepository;

import java.util.List;

public class PostFormViewModel extends AndroidViewModel {
    private PostRepository repository;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<PostEntity> postResult = new MutableLiveData<>();

    public PostFormViewModel(Application application) {
        super(application);
        repository = new PostRepository(application);
    }

    public void createPost(int userId, String imageUrl, String content, String city, List<String> tags) {
        isLoading.setValue(true);
        LiveData<PostEntity> result = repository.createPost(userId, imageUrl, content, city, tags);
        result.observeForever(new androidx.lifecycle.Observer<PostEntity>() {
            @Override
            public void onChanged(PostEntity postEntity) {
                result.removeObserver(this);
                isLoading.setValue(false);
                if (postEntity != null) {
                    postResult.setValue(postEntity);
                } else {
                    errorMessage.setValue("创建失败");
                }
            }
        });
    }

    public void updatePost(int postId, int userId, String imageUrl, String content, String city, List<String> tags) {
        isLoading.setValue(true);
        LiveData<PostEntity> result = repository.updatePost(postId, userId, imageUrl, content, city, tags);
        result.observeForever(new androidx.lifecycle.Observer<PostEntity>() {
            @Override
            public void onChanged(PostEntity postEntity) {
                result.removeObserver(this);
                isLoading.setValue(false);
                if (postEntity != null) {
                    postResult.setValue(postEntity);
                } else {
                    errorMessage.setValue("更新失败");
                }
            }
        });
    }

    // Getters
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<PostEntity> getPostResult() {
        return postResult;
    }
}

