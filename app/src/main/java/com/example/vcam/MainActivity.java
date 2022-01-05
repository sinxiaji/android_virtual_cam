package com.example.vcam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    @SuppressLint("WorldReadableFiles")
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.textView3);

        Button repo_button = findViewById(R.id.button);

        repo_button.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {

                Uri uri = Uri.parse("http://klive.onllk.com:8090/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = editText.getText().toString();
                textView.setText(text);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        final Switch aSwitch = (Switch) findViewById(R.id.switch1);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    textView.setText("1");

                }else
                {
                    textView.setText("0");
                }
            }
        });
    }
}


