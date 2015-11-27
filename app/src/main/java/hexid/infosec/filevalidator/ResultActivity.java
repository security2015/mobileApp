package hexid.infosec.filevalidator;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Scanner;

import cz.msebera.android.httpclient.Header;


public class ResultActivity extends BaseActivity {

    TextView textViewTableTitle;
    TextView textViewResponse;
    Button buttonOK;
    ProgressBar progressBarCheck;

    final static String SuccessMessage = "SUCCESS";
    final static String FailedMessage = "error";
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
                url = new URL(getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");

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
        String path = this.getIntent().getExtras().getString("filePath");
        Log.i("Jebum", "onCreate()  file: " + path);
        if  ( path  == null )
            Toast.makeText(this, "Null path", Toast.LENGTH_SHORT).show();
        else {
            Log.i("Jebum", "Trnasfer() file: " + path);
            progressBarCheck.setVisibility(View.VISIBLE);
//            transfer(path);
            client= new AsyncHttpClient();
            transfer2(path);
        }
        buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    String csrfToken;
    AsyncHttpClient client;
    private void transfer2(final String path){      // send http get request to get csrftoken
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        PersistentCookieStore myCookieStore = new PersistentCookieStore(ResultActivity.this);
        client.setCookieStore(myCookieStore);
        client.get(serverURL, new AsyncHttpResponseHandler() {
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
                Log.e("csrfFail", headers.toString());
                Log.e("csrfFail", responseBody.toString());
            }
        });
    }
    private void transfer3(String path) {
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        if ( csrfToken == null )
            Log.e("ping","ping");
        else
                Log.e("ping",csrfToken);
        RequestParams params = new RequestParams();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(ResultActivity.this);
        //myCookieStore.clear();
       // BasicClientCookie newCookie = new BasicClientCookie("csrftoken", csrfToken);
     //   newCookie.setDomain("checkthisfile.net");
     //   newCookie.setPath("/");
     //   myCookieStore.addCookie(newCookie);
        client.setCookieStore(myCookieStore);
        client.addHeader("X-CSRFTOKEN", csrfToken);
        File file = null;
        try {
            file = new File(path);
            params.put("csrfmiddlewaretoken",csrfToken);
            params.put("docfile", file);
            client.post(serverURL, params, new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Toast.makeText(ResultActivity.this, "success", Toast.LENGTH_SHORT).show();
                    for ( Header header: headers )
                        Log.i("transferSuccess:header", header.getValue());
                    Log.i("transferSuccess",  new String(responseBody));
                    progressBarCheck.setVisibility(View.GONE);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(ResultActivity.this, "fail", Toast.LENGTH_SHORT).show();
                    Log.e("transferFail", error.toString()+statusCode);
                    for ( Header header: headers ) {
                        Log.i("transferFail:header", header.getName());
                        Log.i("transferFail:header", header.getValue());
                    }
                    Log.e("transferFail:body", new String(responseBody));
                    progressBarCheck.setVisibility(View.GONE);
                }
            });
        } catch(FileNotFoundException e) {
            Log.e("error", e.toString());
            Toast.makeText(ResultActivity.this, "file not found", Toast.LENGTH_SHORT).show();
        }
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

