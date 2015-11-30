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
        textViewResponse = (TextView)findViewById(R.id.textViewResponse);
        textViewResult = (TextView)findViewById(R.id.textViewResult);
        textViewTableTitle = (TextView)findViewById(R.id.textViewTableTitle);
        textViewEmbedded = (TextView)findViewById(R.id.textViewEmbedded);
        textViewExtension = (TextView)findViewById(R.id.textViewExtension);
        textViewTableTitle.setText(this.getIntent().getExtras().getString("fileName"));
        String path = this.getIntent().getExtras().getString("filePath");
        Log.i("Jebum", "onCreate()  file: " + path);
        if  ( path  == null )
            Toast.makeText(this, "Null path", Toast.LENGTH_SHORT).show();
        else {
            Log.i("Jebum", "Trnasfer() file: " + path);
            progressBarCheck.setVisibility(View.VISIBLE);
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

    /**
     * @param result
     *
     */
    private void resultProcessing ( String result ) {
        int resultCode = -1;
        String embeddedMessage = "";
        String expectedExtention = "";
        try {
            JSONObject jsonObject= new JSONObject(result);
            resultCode = jsonObject.getInt("result");
            embeddedMessage = jsonObject.getString("tmp value");
            expectedExtention = jsonObject.getString("extension");
        //    Log.i("JSON parsed", resultCode + embeddedMessage + expectedExtention);

        } catch (JSONException e) {
            Log.e("JsonException",e.toString());
            e.printStackTrace();
        }
        switch( resultCode ) {
            case -1:
                Log.e("result:error",result);
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
                textViewResult.setText("Error");
                textViewResult.setTextColor(Color.parseColor("#DB0000")); //red
                break;
            case 0:
                textViewResult.setText("is not supported");
                textViewResult.setTextColor(Color.parseColor("#5D5D5D")); //gray
                break;
            case 1:
                textViewResult.setText("is Normal file");
                textViewResult.setTextColor(Color.parseColor("#47C83E")); //green color
                textViewEmbedded.setText("No files found");
                textViewEmbedded.setTextColor(Color.parseColor("#47C83E")); //green color
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#47C83E")); //green color
                break;
            case 2:
                textViewResult.setText("is suspicious file");
                textViewResult.setTextColor(Color.parseColor("#DB0000")); //red
                textViewEmbedded.setText(embeddedMessage);
                textViewEmbedded.setTextColor(Color.parseColor("#DB0000")); //red
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#47C83E")); //green
                break;
            case 3:
                textViewResult.setText("is suspicious file");
                textViewResult.setTextColor(Color.parseColor("#DB0000")); //red
                textViewEmbedded.setText("No files found");
                textViewEmbedded.setTextColor(Color.parseColor("#47C83E")); //green
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#DB0000")); //red
                break;
            case 4:
                textViewTableTitle.setText("is suspicious file");
                textViewTableTitle.setTextColor(Color.parseColor("#DB0000")); //red
                textViewEmbedded.setText(embeddedMessage);
                textViewEmbedded.setTextColor(Color.parseColor("#DB0000")); //red
                textViewExtension.setText(expectedExtention);
                textViewExtension.setTextColor(Color.parseColor("#DB0000")); //red
                default:
        }


    }

    String csrfToken;
    AsyncHttpClient client;
    private void transfer2(final String path){      // send http get request to obtain csrftoken
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
                for ( Header header: headers )  Log.e("csrfFail", header.toString());
                Log.e("csrfFail", new String(responseBody));
            }
        });
    }
    private void transfer3(String path) {
        String serverURL = getSharedPreferences("preference", MODE_PRIVATE).getString("serverAddress", DefaultURL);
        RequestParams params = new RequestParams();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(ResultActivity.this);
        /*if ( csrfToken == null )
            Log.e("ping","ping");
        else
            Log.e("ping",csrfToken);
*/
        //myCookieStore.clear();
       // BasicClientCookie newCookie = new BasicClientCookie("csrftoken", csrfToken);
     //   newCookie.setDomain("checkthisfile.net");
     //   newCookie.setPath("/");
     //   myCookieStore.addCookie(newCookie);
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