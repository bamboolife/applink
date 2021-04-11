package com.feizhang.applink.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.feizhang.applink.AppLink;
import com.feizhang.applink.AppLinkUtils;
import com.feizhang.applink.PushContentReceiver;
import com.feizhang.applink.RedDotView;

import java.util.Collections;
import java.util.List;

public class OrderDetailActivity extends AppCompatActivity {
    private RedDotView mRedDotView;

    private PushContentReceiver mReceiver = new PushContentReceiver() {

        /**
         * 告诉receiver当前可以接收处理哪些appLink，
         * 在微信聊天场景就好比可以接收文字消息、表情消息、定位消息等
         */
        @Override
        public List<String> getAppLinks() {
            return Collections.singletonList("my-scheme://product/OrderDetail");
        }

        @Override
        public String getAccount(@NonNull Context context) {
            return MyApplication.accountId;
        }

        /**
         * 一般这里我们会再次调用订单接口并刷新当前页面
         * @return true： 终止消息传递，即其他receiver收不到此message
         */
        @SuppressLint("SetTextI18n")
        @Override
        public boolean onReceive(@NonNull Context context, @NonNull AppLink appLink) {
            Toast.makeText(context, "订单已刷新", Toast.LENGTH_SHORT).show();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        TextView newsText = findViewById(R.id.alertText);
        newsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRedDotView.remove();
            }
        });

        mRedDotView = findViewById(R.id.redDotView);
        mRedDotView.setAccount(MyApplication.accountId);
        mRedDotView.setAppLinks("my-scheme://NewMsgAlert");

        findViewById(R.id.sendOrderBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String appLink = "my-scheme://product/OrderDetail?orderId=abc123";
                AppLinkUtils.pushAppLink(v.getContext(), appLink);
            }
        });

        findViewById(R.id.sendNewMsgBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String appLink = "my-scheme://NewMsgAlert";
                AppLinkUtils.pushAppLink(v.getContext(), appLink);
            }
        });

        PushContentReceiver.register(this, mReceiver, true);
    }
}
