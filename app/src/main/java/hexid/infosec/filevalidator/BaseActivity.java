package hexid.infosec.filevalidator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class BaseActivity extends AppCompatActivity {
    final static String DefaultURL = "http://checkthisfile.net/myapp/list/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SharedPreferences pref = getSharedPreferences("preference",MODE_PRIVATE);
        MenuItem item = menu.findItem(R.id.action_monitoring);
        if ( pref.contains("monitoring") ) {
            if ( pref.getBoolean("monitoring", false) )
                item.setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SharedPreferences preferenceSettings = getSharedPreferences("preference", MODE_PRIVATE);
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if ( id == R.id.action_server ) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Server address to uplaod");
            final EditText input = new EditText(this);
            input.setSingleLine();
            input.setText(preferenceSettings.getString("serverAddress", DefaultURL));
            dialogBuilder.setView(input);
            dialogBuilder.setPositiveButton("Set",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (input.getText().toString().equals(""))
                                ;
                            else {
                                SharedPreferences.Editor editor = preferenceSettings.edit().putString("serverAddress", input.getText().toString());
                                editor.commit();
                                Toast.makeText(BaseActivity.this, input.getText().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            AlertDialog dialogServerSetting = dialogBuilder.create();
            dialogServerSetting.show();
            //  editor.putString("serverAddress", TargetURL);
            return true;
        }
        else if ( id == R.id.action_monitoring ) {
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
