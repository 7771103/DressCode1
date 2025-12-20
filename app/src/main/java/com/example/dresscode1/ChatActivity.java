package com.example.dresscode1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.adapter.MessageAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.ChatMessage;
import com.example.dresscode1.network.dto.ChatRequest;
import com.example.dresscode1.network.dto.ChatResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private MessageAdapter messageAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupRecyclerView();
        setupActions();
        
        // 发送欢迎消息
        sendWelcomeMessage();
    }

    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 从底部开始显示
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupActions() {
        // 发送按钮点击事件
        btnSend.setOnClickListener(v -> sendMessage());

        // 输入框回车发送
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        // 监听输入框内容变化，控制发送按钮状态
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                btnSend.setEnabled(hasText);
                btnSend.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void sendWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage("assistant", "你好！我是AI智能助手，有什么可以帮助你的吗？");
        messageAdapter.addMessage(welcomeMessage);
        scrollToBottom();
    }

    private void sendMessage() {
        String messageText = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (messageText.isEmpty()) {
            return;
        }

        // 清空输入框
        etMessage.setText("");

        // 添加用户消息到列表
        ChatMessage userMessage = new ChatMessage("user", messageText);
        messageAdapter.addMessage(userMessage);
        scrollToBottom();

        // 显示加载状态
        setLoading(true);

        // 发送请求到后端
        ChatRequest request = new ChatRequest(currentUserId, messageText, conversationId);
        ApiClient.getService().sendChatMessage(request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ChatResponse chatResponse = response.body();
                            if (chatResponse.isOk() && chatResponse.getReply() != null) {
                                // 保存会话ID
                                if (chatResponse.getConversationId() != null) {
                                    conversationId = chatResponse.getConversationId();
                                }

                                // 添加AI回复到列表
                                ChatMessage assistantMessage = new ChatMessage("assistant", chatResponse.getReply());
                                messageAdapter.addMessage(assistantMessage);
                                scrollToBottom();
                            } else {
                                Toast.makeText(ChatActivity.this, 
                                    chatResponse.getMsg() != null ? chatResponse.getMsg() : "获取回复失败", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ChatActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(ChatActivity.this, "发送失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
        btnSend.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void scrollToBottom() {
        rvMessages.post(() -> {
            if (messageAdapter.getItemCount() > 0) {
                rvMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
    }
}

