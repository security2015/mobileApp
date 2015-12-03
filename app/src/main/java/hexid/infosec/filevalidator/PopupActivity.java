package hexid.infosec.filevalidator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

// on service detect the suspicious downloaded file.
public class PopupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final String json = getIntent().getExtras().getString("json");
        String fileName="";
        try {
            fileName = new JSONObject(json).getString("original");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dialogBuilder.setTitle("CheckEr");
        dialogBuilder.setMessage("Suspicous file detected:"+System.lineSeparator()+fileName);
        dialogBuilder.setPositiveButton("Show details",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(PopupActivity.this, ResultActivity.class);
                        intent.putExtra("json", json);
                        startActivity(intent);
                        finish();
                    }
                });
        dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        AlertDialog dialogServerSetting = dialogBuilder.create();
        dialogServerSetting.show();
    }

}
