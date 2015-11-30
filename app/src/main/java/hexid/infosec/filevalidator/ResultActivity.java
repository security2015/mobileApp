package hexid.infosec.filevalidator;

import android.graphics.Color;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;


public class ResultActivity extends BaseActivity {

    TextView textViewTableTitle;
    TextView textViewResponse;
    TextView textViewEmbedded;
    TextView textViewExtension;
    TextView textViewResult;
    Button buttonOK;
    ProgressBar progressBarCheck;

    final static String SuccessMessage = "SUCCESS";
    final static String FailedMessage = "error";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        progressBarCheck = (ProgressBar)findViewById(R.id.progressBarCheck);
        textViewResult = (TextView)findViewById(R.id.textViewResult);
        textViewTableTitle = (TextView)findViewById(R.id.textViewTableTitle);
        textViewEmbedded = (TextView)findViewById(R.id.textViewEmbedded);
        textViewExtension = (TextView)findViewById(R.id.textViewExtension);
        if ( getIntent().getExtras().getString("json") != null ) {
            resultProcessing(getIntent().getExtras().getString("json"));
        }
        else {
            textViewTableTitle.setText(this.getIntent().getExtras().getString("fileName"));
            String path = this.getIntent().getExtras().getString("filePath");
            Log.i("Jebum", "onCreate()  file: " + path);
            if (path == null)
                Toast.makeText(this, "Null path", Toast.LENGTH_SHORT).show();
            else {
                Log.i("Jebum", "Trnasfer() file: " + path);
                progressBarCheck.setVisibility(View.VISIBLE);
                client = new AsyncHttpClient();
                readyTransfer(path);
            }
        }
        buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * @param result
     *
     */
    private void resultProcessing ( String result ) {
        int extensionResult = -1;
        int embeddedResult = -1;
        String embeddedMessage = "";
        String expectedExtention = "";
        String fileName = "";
        try {
            JSONObject jsonObject= new JSONObject(result);
            extensionResult = jsonObject.getInt("extension_result");
            embeddedResult = jsonObject.getInt("embedded_result");
            embeddedMessage = jsonObject.getString("embedded_message");
            expectedExtention = jsonObject.getString("expected");
            fileName = jsonObject.getString("original");
        //    Log.i("JSON parsed", resultCode + embeddedMessage + expectedExtention);

        } catch (JSONException e) {
            Log.e("JsonException",e.toString());
            e.printStackTrace();
        }
        if ( fileName != null || !fileName.equals("") )
             textViewTableTitle.setText(fileName);
        if ( extensionResult == 1 && embeddedResult == 1 ) {
            textViewResult.setText("is Normal file");
            textViewResult.setTextColor(Color.parseColor("#47C83E")); //green color
        }
        else if ( extensionResult == 0 ) {
            textViewResult.setText("is Not Supported file");
            textViewResult.setTextColor(Color.parseColor("#DB0000")); //red
        }
        else {
            textViewResult.setText("is Suspicous file");
            textViewResult.setTextColor(Color.parseColor("#DB0000"));
        }
        switch( extensionResult ) {
            case 0:
                textViewExtension.setText("Not supported");
                textViewExtension.setTextColor(Color.parseColor("#DB0000"));
                break;
            case 1:
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#47C83E")); //green color
                break;
            case 2:
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#DB0000")); //red
                break;
            default:
                textViewExtension.setText("No information");
                textViewExtension.setTextColor(Color.parseColor("#777777"));
        }
        switch (embeddedResult) {
            case 0:
                textViewEmbedded.setText("Not supported");
                textViewEmbedded.setTextColor(Color.parseColor("#DB0000")); //red
                break;
            case 1:
                textViewEmbedded.setText("No file found.");
                textViewEmbedded.setTextColor(Color.parseColor("#47C83E")); //green color
                break;
            case 2:
                textViewEmbedded.setText(embeddedMessage);
                textViewEmbedded.setTextColor(Color.parseColor("#DB0000")); //red
                break;
            default:
                textViewEmbedded.setText("No information");
                textViewEmbedded.setTextColor(Color.parseColor("#777777"));
        }
    }

    String csrfToken;
    AsyncHttpClient client;
    private void readyTransfer(final String path){      // send http get request to obtain csrftoken
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
                        transferFile(path);
                        return;
                    }
                }
                Log.e("csrfFail", "Fail to get csrf token");
                transferFile(path);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("csrfFail", error.toString());
                for ( Header header: headers )  Log.e("csrfFail", header.toString());
                Log.e("csrfFail", new String(responseBody));
            }
        });
    }
    private void transferFile(String path) {
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        RequestParams params = new RequestParams();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(ResultActivity.this);
        client.setCookieStore(myCookieStore);
        client.addHeader("X-CSRFTOKEN", csrfToken);
        File file;
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
                    //Toast.makeText(ResultActivity.this, "success", Toast.LENGTH_SHORT).show();
                    for ( Header header: headers ) Log.i("transferSuccess:header", header.toString());
                    Log.i("transferSuccess",  new String(responseBody));
                    resultProcessing(new String(responseBody));
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