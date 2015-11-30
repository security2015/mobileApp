package hexid.infosec.filevalidator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

// on service detect the suspicious downloaded file.
public class PopupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Suspicous file detected");
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setText("Caution");
        dialogBuilder.setView(input);
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().toString().equals(""))
                            ;
                        else {
                            Toast.makeText(PopupActivity.this, input.getText().toString(), Toast.LENGTH_SHORT).show();
                        }
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
