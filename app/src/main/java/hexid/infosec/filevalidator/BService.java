package hexid.infosec.filevalidator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class BService extends Service {

    /**
     *  FileObserver: Code Snippet from http://dev.re.kr/m/post/62
     */
    //ArrayList<> listFileDownloadFolder =
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
            Log.i("TestFileObserver", strEvents.toString());
            if (  event ==  FileObserver.CREATE ) {
                String createdFile="";
                if ( createdFile.substring(createdFile.lastIndexOf('.'), createdFile.length()).equals("crdownload") )
                    ;
                else
                    ;
            }
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
/*
    static ArrayList<String> fileList;
    static String getCreatedFile () {
        File f = new File(Environment.getExternalStorageDirectory(), "Downloads");
        if ( fileList == null ) {
            fileList = new ArrayList<String>(Arrays.asList(f.list()));
            return "";
        }
        ArrayList<String> downloads = new ArrayList<String>(Arrays.asList(f.list()));

        String createdFile = "";
        for ( String fileName : downloads )
            if ( !fileList.contains(fileName))
                createdFile = fileName;
        return createdFile;
    }
    */
    private void downloadObserved ()  {
        Builder mBuilder = (Builder) new Builder(this).setSmallIcon(R.drawable.search_icon).setContentTitle("CheckEr")
                        .setContentText("File Downloaded");
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ResultActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }
    private void showPopUpNotification () {
        // Make popup(Call PopupActivity)
        Intent popupIntent = new Intent(this, PopupActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(popupIntent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String downloadPath = Environment.getExternalStorageDirectory()+"/Download";
        // Monitoring file chage
        monitorAllFiles(new File(Environment.getExternalStorageDirectory() + "/Download"));
        Toast.makeText(this, "Monitoring On", Toast.LENGTH_SHORT).show();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopMonitoring(new File(Environment.getExternalStorageDirectory()+"/Download"));
        Toast.makeText(this, "Monitoring Off", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}