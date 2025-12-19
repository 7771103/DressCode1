package com.example.dresscode1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.ChatMessage;
import com.example.dresscode1.network.dto.ChatRequest;
import com.example.dresscode1.network.dto.ChatResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgentFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private EditText etInput;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private int currentUserId = 0;
    
    public static AgentFragment newInstance(int currentUserId) {
        AgentFragment fragment = new AgentFragment();
        Bundle args = new Bundle();
        args.putInt("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getInt("currentUserId", 0);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agent, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        etInput = view.findViewById(R.id.etInput);
        btnSend = view.findViewById(R.id.btnSend);
        progressBar = view.findViewById(R.id.progressBar);
        
        adapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // 添加欢迎消息
        addWelcomeMessage();
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        return view;
    }
    
    private void addWelcomeMessage() {
        ChatMessage welcome = new ChatMessage();
        welcome.setRole("assistant");
        welcome.setContent("你好！我是你的穿搭智能助手，可以为你提供穿搭建议、搭配推荐等服务。有什么可以帮助你的吗？");
        messageList.add(welcome);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
    }
    
    private void sendMessage() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            return;
        }
        
        // 添加用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(input);
        messageList.add(userMessage);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
        
        // 清空输入框
        etInput.setText("");
        
        // 显示加载状态
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        
        // 发送到AI
        ApiService apiService = ApiClient.getService();
        ChatRequest request = new ChatRequest(currentUserId, input, messageList);
        Call<ChatResponse> call = apiService.chat(request);
        
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ChatResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        // 添加AI回复
                        ChatMessage aiMessage = new ChatMessage();
                        aiMessage.setRole("assistant");
                        aiMessage.setContent(body.getData().getContent());
                        messageList.add(aiMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.smoothScrollToPosition(messageList.size() - 1);
                    } else {
                        Toast.makeText(getContext(), body.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

