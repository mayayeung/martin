package mayayeung.github.com.martin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mayayeung.utils.MiscUtils;

import java.util.Date;

public class MainActivity extends Activity {
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = (TextView) findViewById(R.id.textInfo);
        info.setText(MiscUtils.formatDate(new Date()));
    }


}
