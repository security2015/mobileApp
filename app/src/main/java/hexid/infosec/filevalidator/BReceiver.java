package hexid.infosec.filevalidator;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (  intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Launch the service at boot
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
        else  if ( intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) ) {
            // Auto-check downloaded file
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
