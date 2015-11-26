package hexid.infosec.filevalidator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;


public class ResultActivity extends BaseActivity {

    TextView textViewTableTitle;
    TextView textViewResponse;
    Button buttonOK;
    ProgressBar progressBarCheck;

    final static String SuccessMessage = "SUCCESS";
    final static String FailedMessage = "error";
    final static String targetURL = "http://61.72.174.90";

    String httpResponse = "";

    void transfer(String filePath) {
        transfer(new File(filePath));
    }
    void transfer(File f) {
        TransferThread thread = new TransferThread();
        thread.setF(f);
        thread.start();
    }

    // Network I/O thread
    class TransferThread extends Thread {
        File f;
        public void setF(File file) {
            f = file;
        }
        public void run() {
            try {
                String boundary = "^******^"; // Data boundary
                String delimiter = "\r\n--" + boundary + "\r\n";
                StringBuffer postDataBuilder = new StringBuffer();
                postDataBuilder.append(delimiter);
                postDataBuilder.append(setValue("teamName", "hexid"));
                postDataBuilder.append(delimiter);
                // File append
                postDataBuilder.append(setFile("docfile", f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('/') + 1, f.getAbsolutePath().length())));
                postDataBuilder.append("\r\n");
                URL url;
                byte[] unitByte;
                url = new URL(getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", targetURL));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setChunkedStreamingMode(0);
                conn.setRequestMethod("POST");  // Use http POST method
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                outputStream.writeUTF(postDataBuilder.toString());

                //Base64OutputStream base64OutputStream = new Base64OutputStream(outputStream, Base64.NO_CLOSE);
                BufferedOutputStream buffer = new BufferedOutputStream(outputStream);
                FileInputStream fileInputStream = new FileInputStream(f);
                unitByte = new byte[1024];
                while ( fileInputStream.read(unitByte) != -1 ) {
                    buffer.write(unitByte);
                }
                fileInputStream.close();
                outputStream.writeBytes(delimiter);
                outputStream.close();

                InputStream inputStream = conn.getInputStream();
                Scanner scanner = new Scanner(inputStream);
                while( scanner.hasNext() ) {
                    String response = scanner.nextLine();
                    // JB:간단한 html parser..test용.
                    /*if ( response.contains("<body>") ) {
                        response = response.substring(response.indexOf("<body>")+6, response.lastIndexOf("</body"));
                    }*/
                    if ( response != null && response.equalsIgnoreCase(SuccessMessage)) {
                        httpResponse = SuccessMessage;
                    }
                    else {
                        httpResponse = response;
                    }
                }
                conn.disconnect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            textViewResponse.setText(httpResponse);
                            textViewTableTitle.setText(f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('/') + 1, f.getAbsolutePath().length()));
                            Toast.makeText(ResultActivity.this, httpResponse, Toast.LENGTH_SHORT).show();
                            progressBarCheck.setVisibility(View.GONE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String setValue(String key, String value) {
            return "Content-Disposition: form-data; name=\"" + key + "\"r\n\r\n"
                    + value;
        }
        String setFile(String key, String fileName) {
            return "Content-Disposition: form-data; name=\"" + key
                    + "\";filename=\"" + fileName + "\"\r\n";
        }
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        progressBarCheck = (ProgressBar)findViewById(R.id.progressBarCheck);
        textViewResponse = (TextView)findViewById(R.id.textViewResponse);
        String  path = this.getIntent().getExtras().getString("filePath");
        Log.i("Jebum", "onCreate()  file: " + path);
        if  ( path  == null )
            Toast.makeText(this, "Null path", Toast.LENGTH_SHORT).show();
        else {
            Log.i("Jebum", "Trnasfer()  file: "+path);
            progressBarCheck.setVisibility(View.VISIBLE);
//            transfer(path);
            transfer2(path);
        }
        buttonOK = (Button)findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
    /*
    private String getCsrfFromUrl(String url) {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse httpResponse = httpClientStatic.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();

        List<Cookie> cookies = httpClientStatic.getCookieStore().getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("csrftoken")) {
                return cookie.getValue();
            }
        }
        return null;  // Or throw exception.
    }*/
    String csrfToken;
    private void transfer2(String path){
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", targetURL);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        BasicClientCookie newCookie = new BasicClientCookie("csrftoken", "JShAhinx8XCOxatTApVG2NVj2rH3xOye");
        newCookie.setVersion(1);
        newCookie.setDomain("checkthisfile.net");
        newCookie.setPath("/");
        newCookie.setAttribute("csrftoken", "JShAhinx8XCOxatTApVG2NVj2rH3xOye");
        myCookieStore.addCookie(newCookie);
        client.setCookieStore(myCookieStore);
        File file = null;
        try {
            file = new File(path);
            params.put("csrfmiddlewaretoken","JShAhinx8XCOxatTApVG2NVj2rH3xOye");
            params.put("X-CSRFTOKEN", "J22Ahinx8XCOxatTApVG2NVj2rH3xOye");
            params.put("docfile", file);
            //URL url = new URL(getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", targetURL));
            client.post(getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", targetURL), params, new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Toast.makeText(ResultActivity.this, "success", Toast.LENGTH_SHORT).show();
                    progressBarCheck.setVisibility(View.GONE);
                    Log.i("info", new String(responseBody));
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(ResultActivity.this, "fail", Toast.LENGTH_SHORT).show();
                    progressBarCheck.setVisibility(View.GONE);
                    Log.e("jj", error.toString());
                    Log.e("jj", headers.toString());
                    Log.e("jj", responseBody.toString());
                }
            });
        } catch(FileNotFoundException e){
            Log.e("error", e.toString());
            Toast.makeText(ResultActivity.this, "file not found", Toast.LENGTH_SHORT).show();
        } //catch (MalformedURLException e) {
           // e.printStackTrace();
       // }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}

