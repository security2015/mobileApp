package hexid.infosec.filevalidator;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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


public class ResultActivity extends Activity {

    TextView textViewResponse;

    final static String SuccessMessage = "SUCCESS";
    final static String FailedMessage = "error";
    final static String targetURL = "http://61.72.174.90/andTest";

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
                URL url;
                byte[] unitByte;
                Base64OutputStream base64OutputStream;
                url = new URL(getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", targetURL));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setChunkedStreamingMode(0);
                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                base64OutputStream = new Base64OutputStream(outputStream, Base64.NO_CLOSE);
                BufferedOutputStream buffer = new BufferedOutputStream(base64OutputStream);
                FileInputStream fileInputStream = new FileInputStream(f);

                unitByte = new byte[1024];
                while ( fileInputStream.read(unitByte) != -1 ) {
                    buffer.write(unitByte);
                }
                fileInputStream.close();

                InputStream inputStream = conn.getInputStream();
                Scanner scanner = new Scanner(inputStream);
                while( scanner.hasNext() ) {
                    String response = scanner.nextLine();

                    // JB:간단한 html parser..test용.
                    if ( response.contains("<body>") ) {
                        response = response.substring(response.indexOf("<body>")+6, response.lastIndexOf("</body"));
                    }
                    if ( response != null && response.equalsIgnoreCase(SuccessMessage)) {
                        httpResponse = SuccessMessage;
                    }
                    else {
                        httpResponse = response;
                    }
                }
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        try{
                            textViewResponse.setText(httpResponse);
                            Toast.makeText(ResultActivity.this
                                    , httpResponse, Toast.LENGTH_SHORT).show();
                        }catch(Exception e){
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
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        textViewResponse = (TextView)findViewById(R.id.textViewResponse);
        String  path = this.getIntent().getExtras().getString("filePath");
        transfer(path);
    }
}
