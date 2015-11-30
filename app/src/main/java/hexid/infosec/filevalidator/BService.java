package hexid.infosec.filevalidator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;


public class BService extends Service {

    final static String DefaultURL = "http://checkthisfile.net/myapp/list/";

    /**
     *  FileObserver: Code Snippet from http://dev.re.kr/m/post/62
     */

    public static final ArrayList<TestFileObserver> sListFileObserver = new ArrayList<TestFileObserver>();
    class TestFileObserver extends FileObserver {
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
                    downloadObserved();
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

    private void downloadObserved ( )  {

        String downloadPath = Environment.getExternalStorageDirectory()+"/Download";
        String strFilePath = lastFileModified(downloadPath);
        if ( strFilePath.substring(strFilePath.lastIndexOf('.'), strFilePath.length()).equals("crdownload") )
            ;
        else if ( strFilePath.contains(".com.google.Chrome."))
            ;
        else {
            transfer2(strFilePath);
        }
    }
    private void resultProcessing ( String JSON ) {
        int extensionResult = -1;
        int embeddedResult = -1;
        try {
            JSONObject j = new JSONObject(JSON);
            JSONObject jsonObject= new JSONObject(JSON);
            extensionResult = jsonObject.getInt("extension_result");
            embeddedResult = jsonObject.getInt("embedded_result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if ( extensionResult == 1 && embeddedResult == 1 )
            showNotification(JSON);
        else
            showPopUpNotification(JSON);


    }
    private void showNotification ( String JSON ) {
        String fileName="";
        try {
            fileName = new JSONObject(JSON).getString("original");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent resultIntent = new Intent(this, ResultActivity.class);
        resultIntent.putExtra("json", JSON);
        Builder mBuilder = (Builder) new Builder(this).setSmallIcon(R.drawable.icon_search)
                .setContentTitle("File Downloaded: " + fileName)
                .setContentText(" Approved as safe file w/ CheckEr");
        BitmapDrawable d = (BitmapDrawable)getResources().getDrawable(R.drawable.icon_search);
        mBuilder.setLargeIcon(d.getBitmap());
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
    private void showPopUpNotification ( String JSON ) {
        // Make popup: Call PopupActivity
        Intent popupIntent = new Intent(this, PopupActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        popupIntent.putExtra("json", JSON);
        startActivity(popupIntent);
    }

    private String lastFileModified(String dir) {
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice.getAbsolutePath();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String downloadPath = Environment.getExternalStorageDirectory()+"/Download";
        // Monitoring file chage
        monitorAllFiles(new File(Environment.getExternalStorageDirectory() + "/Download"));
        Toast.makeText(this, "Monitoring On", Toast.LENGTH_SHORT).show();
        client = new AsyncHttpClient();
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

    String csrfToken;
    AsyncHttpClient client;
    private void transfer2(final String path){      // send http get request to obtain csrftoken
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        PersistentCookieStore myCookieStore = new PersistentCookieStore(BService.this);
        client.setCookieStore(myCookieStore);
        client.get(serverURL, new AlwaysAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                for (Header header : headers) {
                    if (header.getName().equals("Set-Cookie")) {
                        String cookieVal = header.getValue();
                        csrfToken = cookieVal.substring(cookieVal.indexOf("csrftoken=") + 10, cookieVal.indexOf(';'));
                        Log.e("csrfSuccess", csrfToken);
                        transfer3(path);
                        return;
                    }
                }
                Log.e("csrfFail", "Fail to get csrf token");
                transfer3(path);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("csrfFail", error.toString());
                for ( Header header: headers )  Log.e("csrfFail", header.toString());
                Log.e("csrfFail", new String(responseBody));
            }
        });
    }
    private void transfer3(String path) {
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        RequestParams params = new RequestParams();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(BService.this);
        client.setCookieStore(myCookieStore);
        client.addHeader("X-CSRFTOKEN", csrfToken);
        File file;
        try {
            file = new File(path);
            params.put("csrfmiddlewaretoken",csrfToken);
            params.put("docfile", file);
            client.post(serverURL, params, new AlwaysAsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    for ( Header header: headers ) Log.i("transferSuccess:header", header.toString());
                    Log.i("transferSuccess", new String(responseBody));
                    resultProcessing(new String(responseBody));
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(BService.this, "fail", Toast.LENGTH_SHORT).show();
                    Log.e("transferFail", error.toString()+statusCode);
                    for ( Header header: headers ) {
                        Log.i("transferFail:header", header.getName());
                        Log.i("transferFail:header", header.getValue());
                    }
                    Log.e("transferFail:body", new String(responseBody));
                }
            });
        } catch(FileNotFoundException e) {
            Log.e("error", e.toString());
            Toast.makeText(BService.this, "file not found", Toast.LENGTH_SHORT).show();
        }
    }
}
