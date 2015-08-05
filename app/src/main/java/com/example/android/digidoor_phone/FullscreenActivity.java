package com.example.android.digidoor_phone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class FullscreenActivity extends Activity {

    private Handler mHandler;
    private ViewFlipper viewFlipper;

    private static final int LOCK = 2;
    private static final int UNLOCK = 1;
    private static int REQUEST_CODE = 301;
    public static boolean END_CALL = false;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;

    //GET Request URL to remotely unlock the gate.
    private String urlRemoteUnlock ="http://digidoor.herokuapp.com/api/v1/unlocks/unlock.json";
    //GET Request URL to remotely lock the gate.
    private String urlRemoteLock ="http://digidoor.herokuapp.com/api/v1/unlocks/lock.json";
    //GET Request URL which defines the pin.
    private String urlOwners ="http://digidoor.herokuapp.com/api/v1/owners.json";
    public static List<String> pinList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(getApplicationContext(),
                "Gate is now locked.", Toast.LENGTH_SHORT).show();

        setContentView(R.layout.activity_fullscreen2);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

        // Hides the Action Bar.
        getActionBar().hide();

        //retrieves list of owners' pins.
        requestPin(urlOwners);

        //Invoke NumbPad fragment to prompt for pin.
        setupNumbpad();

        //This buttons initiates remote unlocking.
        Button buttonRemoteUnlock = (Button) findViewById(R.id.button_remoteUnlock);
        buttonRemoteUnlock.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.showPrevious();

                //sends unlock protocol to gate
                sendRemoteCommand(urlRemoteUnlock);

                setupNumbpad();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent mStartActivity = new Intent(getApplicationContext(), FullscreenActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(
                                getApplicationContext(), mPendingIntentId,
                                mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager
                                mgr = (AlarmManager)getApplicationContext().getSystemService(
                                getApplicationContext().ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    }
                }, 8000);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        sendRemoteCommand(urlRemoteLock);


    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /***
     * This method invokes the Numbpad Dialog on top of the main activity.
     */
    private void setupNumbpad() {

        // create an instance of NumbPad
        NumbPad np = new NumbPad();

        // optionally set additional title
        np.setAdditionalText("Please Enter Pin:");

        // show the NumbPad to capture input.
        np.show(this, "REMOTE UNLOCKER", NumbPad.HIDE_INPUT,
                new NumbPad.numbPadInterface() {

                    // This is called when the user click the 'unlock' button on the dialog
                    // value is the captured input from the dialog.
                    public String numPadInputValue(String value) {

                        boolean pinValid = false;
                        String usedPin = "";

                        for (String pin : pinList) {
                            if (value.equals(pin)) {
                                pinValid = true;
                                usedPin = value;
                            }
                        }

                        if (pinValid) {
                            viewFlipper.showNext();

                            Toast.makeText(getApplicationContext(),
                                    "Pin: " + usedPin + ". Pin is correct, please enter.", Toast.LENGTH_SHORT).show();
                        } else {
                            // generate a toast message to inform the user that
                            // the captured input is not valid
                            Toast.makeText(getApplicationContext(),
                                    "Pin is incorrect, please try again.", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getApplicationContext(), FullscreenActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        return null;
                    }

                    // This is called when the user clicks the 'Cancel' button on the dialog
                    public String numPadCanceled() {
                        // generate a toast message to inform the user that the pin
                        // capture was canceled
                        Toast.makeText(getApplicationContext(),
                                "Pin capture canceled!", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                });
    }


    /***
     * This method takes in the HTTP address and performs a GET request
     * to retrieve the pin from the server database.
     * @param uri
     * GET Request URL which defines the pin.
     */
    private void requestPin(String uri){

        JsonArrayRequest request = new JsonArrayRequest(uri,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject object = response.getJSONObject(i);
                                pinList.add(Integer.toString(object.getInt("pin")));

                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(),
                                        "JSONException", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        Toast.makeText(getApplicationContext(),
                                "Volley Error!", Toast.LENGTH_SHORT).show();
                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    /***
     * This method takes in the HTTP address and performs a GET request
     * to send unlocking or locking instructions to gate.
     * @param uri
     * GET Request URL which defines either lock or unlock.
     */
    private void sendRemoteCommand(String uri){

        JsonArrayRequest request = new JsonArrayRequest(uri,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError ex) {
                Toast.makeText(getApplicationContext(),
                        "Remote Command Error!", Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}

