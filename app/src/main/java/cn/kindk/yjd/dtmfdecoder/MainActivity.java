package cn.kindk.yjd.dtmfdecoder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Button stateButton;
    private Button clearButton;
    private TextView ssidTextView;
    private TextView pwdTextView;

    Controller controller;

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controller = new Controller(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no permission of recorder ");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }



        stateButton = (Button) findViewById(R.id.stateButton);
        clearButton = (Button) findViewById(R.id.clearButton);
        ssidTextView = (TextView) findViewById(R.id.ssidTextView);
        pwdTextView = (TextView) findViewById(R.id.pwdTextView);

        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.changeState();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.clear();
            }
        });

        controller.changeState();
    }

    public void start() {
        stateButton.setText(R.string.stop);
    }

    public void stop() {
        stateButton.setText(R.string.start);
    }

    public void setSSID(String str) {
        ssidTextView.setText(str);
    }

    public void setPWD(String str) {
        pwdTextView.setText(str);
    }

    public void clear() {
        ssidTextView.setText("");
        pwdTextView.setText("");
    }
}
