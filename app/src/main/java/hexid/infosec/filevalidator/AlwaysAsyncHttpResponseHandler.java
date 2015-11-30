package hexid.infosec.filevalidator;

import com.loopj.android.http.AsyncHttpResponseHandler;

public abstract class AlwaysAsyncHttpResponseHandler extends AsyncHttpResponseHandler {
    @Override
    public boolean getUseSynchronousMode() {
        return false;
    }
}
