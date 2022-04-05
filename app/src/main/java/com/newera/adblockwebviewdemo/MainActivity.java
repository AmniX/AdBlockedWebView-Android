package com.newera.adblockwebviewdemo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.amnix.adblockwebview.ui.AdBlocksWebViewActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdBlocksWebViewActivity.init(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.go_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdBlocksWebViewActivity.startWebView(MainActivity.this,((EditText)findViewById(R.id.edittext)).getText().toString(),getResources().getColor(R.color.colorPrimary));
            }
        });
    }


}
