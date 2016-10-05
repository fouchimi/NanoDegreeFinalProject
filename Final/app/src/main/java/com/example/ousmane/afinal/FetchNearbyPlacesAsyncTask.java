package com.example.ousmane.afinal;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ousma on 9/28/2016.
 */

public class FetchNearbyPlacesAsyncTask extends AsyncTask<String, Void, List<List<HashMap<String,String>>>> {
    private List<List<HashMap<String,String>>> routes = null;
    private final static String TAG = FetchNearbyPlacesAsyncTask.class.getSimpleName();
    private Context mContext;
    JSONObject jObject;

    public FetchNearbyPlacesAsyncTask(Context context) {
        mContext = context;
    }
    @Override
    protected List<List<HashMap<String,String>>> doInBackground(String... params) {
        String result =  new HttpManager().getData(params[0]);
        try {
            jObject = new JSONObject(result);
            Log.d("ParserTask",result);
            DataParser parser = new DataParser();
            Log.d("ParserTask", parser.toString());
            // Starts parsing data
            routes = parser.parse(jObject);
            Log.d("ParserTask","Executing routes");
            Log.d("ParserTask",routes.toString());

        } catch (Exception e) {
            Log.d("ParserTask",e.toString());
            e.printStackTrace();
        }
        return routes;
    }

    @Override
    protected void onPostExecute(List<List<HashMap<String,String>>> routes) {
        super.onPostExecute(routes);
        if(routes != null && routes.size() > 0) {
            MainActivity activity = (MainActivity) mContext;
            activity.onComplete(routes);
        }

    }

    public interface Listener {
        void onComplete(List<List<HashMap<String,String>>> routes);
    }
}
