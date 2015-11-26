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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


public class ResultActivity extends BaseActivity {

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
                String boundary = "^******^";
                // Data boundary
                String delimiter = "\r\n--" + boundary + "\r\n";
                StringBuffer postDataBuilder = new StringBuffer();
                postDataBuilder.append(delimiter);
                postDataBuilder.append(setValue("teamName", "hexid"));
                postDataBuilder.append(delimiter);
                // File append
                postDataBuilder.append(setFile("fileUploaded", f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('/') + 1, f.getAbsolutePath().length())));
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
            transfer(path);
        }
        buttonOK = (Button)findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

