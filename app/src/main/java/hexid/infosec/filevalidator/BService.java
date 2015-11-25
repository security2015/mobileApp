package hexid.infosec.filevalidator;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;


public class BService extends Service {

    /**
     *  FileObserver: Code Snippet from http://dev.re.kr/m/post/62
     */
    public static final ArrayList<TestFileObserver> sListFileObserver = new ArrayList<TestFileObserver>();
    static class TestFileObserver extends FileObserver {
        private String mPath;

        int[] eventValue = new int[] {FileObserver.ACCESS, FileObserver.ALL_EVENTS, FileObserver.ATTRIB, FileObserver.CLOSE_NOWRITE,FileObserver.CLOSE_WRITE, FileObserver.CREATE,
                FileObserver.DELETE, FileObserver.DELETE_SELF,FileObserver.MODIFY,FileObserver.MOVED_FROM,FileObserver.MOVED_TO, FileObserver.MOVE_SELF,FileObserver.OPEN};
        String[] eventName = new String[] {"ACCESS", "ALL_EVENTS", "ATTRIB", "CLOSE_NOWRITE", "CLOSE_WRITE", "CREATE",
                "DELETE", "DELETE_SELF" , "MODIFY" , "MOVED_FROM" ,"MOVED_TO", "MOVE_SELF","OPEN"};


        public TestFileObserver(String path) {
            super(path);
            mPath = path;
            sListFileObserver.add(this);
        }
        public TestFileObserver(String path, int mask) {
            super(path, mask);
            mPath = path;
            sListFileObserver.add(this);
        }

        @Override
        public void onEvent(int event, String path) {
            StringBuilder strEvents = new StringBuilder();
            strEvents.append("Event : ").append('(').append(event).append(')');
            for(int i = 0; i < eventValue.length; ++i) {
                if(event == eventValue[i]) {
                    strEvents.append(eventName[i]);
                    strEvents.append(',');
                }
            }
            if((event & FileObserver.DELETE_SELF) == FileObserver.DELETE_SELF) {
                sListFileObserver.remove(this);
            }
            strEvents.append("\tPath : ").append(path).append('(').append(mPath).append(')');
            Log.i("TestFileObserver",strEvents.toString());
        }
    }
    private void monitorAllFiles(File root) {
        File[] files = root.listFiles();
        TestFileObserver fileObserver = new TestFileObserver(root.getAbsolutePath());
        fileObserver.startWatching();
        for(File file : files) {
            fileObserver = new TestFileObserver(file.getAbsolutePath());
            fileObserver.startWatching();
            if(file.isDirectory()) monitorAllFiles(file);
        }
    }
    private void stopMonitoring(File root) {
        File[] files = root.listFiles();
        for(File file : files) {
            TestFileObserver fileObserver = new TestFileObserver(file.getAbsolutePath());
            fileObserver.stopWatching();
            if(file.isDirectory()) monitorAllFiles(file);
        }
    }
    BroadcastReceiver mybroadcast;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        monitorAllFiles(new File(Environment.getExternalStorageDirectory()+"/Download"));

        // BroadcastReciever start
        Toast.makeText(this, "Service On", Toast.LENGTH_SHORT).show();
        mybroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                Log.i("[BroadcastReceiver]", "MyReceiver " + intent.getAction());
                if ( intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) )
                    Toast.makeText(context, "Downloaded", Toast.LENGTH_LONG).show();
                if ( intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) )
                    Toast.makeText(context, "Booted", Toast.LENGTH_LONG).show();
            }
        };
        registerReceiver(mybroadcast, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(mybroadcast, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        registerReceiver(mybroadcast, new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
        registerReceiver(mybroadcast, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mybroadcast);
        // BroadcastReciever stop
        Toast.makeText(this, "Service Off", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
