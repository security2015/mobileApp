package hexid.infosec.filevalidator;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 *  H.E.X.I.D 2015
 */

public class MainActivity extends AppCompatActivity {
    private String strFilePath;
    TextView textViewFileName;
    Button buttonCheck;
    final static String TargetURL = "http://61.72.174.90/andTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Sever address initialize
        SharedPreferences pref = getSharedPreferences("preference", MODE_PRIVATE);
        if ( !pref.contains("serverAddress") ){
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("serverAddress", TargetURL);
            editor.commit();
        }
        // Monitoring service auto-start according to preference
        if ( pref.contains("monitoring") ) {
            if ( pref.getBoolean("monitoring", false) ) {
                Intent intent = new Intent(this, BService.class);
                startService(intent);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewFileName = (TextView)findViewById(R.id.textViewFileName);
        buttonCheck = (Button)findViewById(R.id.buttonCheck);
        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void check() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("filePath", strFilePath);
        startActivity(intent);
    }
    public void openExplorer(View arg0){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    strFilePath = data.getData().getPath();
                    Log.i("path", strFilePath);
                    String fileName = strFilePath.substring(strFilePath.lastIndexOf('/')+1,strFilePath.length());
                    textViewFileName.setText(fileName);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if ( id == R.id.action_server ) {
            SharedPreferences pref = getSharedPreferences("preference", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("serverAddress", TargetURL);
            editor.commit();
            return true;
        }
        else if ( id == R.id.action_service ) {
            SharedPreferences pref = getSharedPreferences("preference", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            if (item.isChecked()) {
                item.setChecked(false);
                editor.putBoolean("monitoring", false);
                editor.commit();
                //  Destroy service
                Intent intent = new Intent(this, BService.class);
                stopService(intent);
                return false;
            } else {
                item.setChecked(true);
                editor.putBoolean("monitoring", true);
                editor.commit();
                // Start service
                Intent intent = new Intent(this, BService.class);
                startService(intent);
                return false;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
