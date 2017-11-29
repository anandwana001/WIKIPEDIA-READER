package com.riteshmohapatra.wikipediareader;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RequestQueue queue;
    private TextToSpeech tts;

    // search
    private Toolbar toolbar;
    private MenuItem searchBtn;
    private boolean isSearchOpen;

    // viewer
    TextView textView;
    ProgressBar progress;
    View textViewer;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.queue = Volley.newRequestQueue(MainActivity.this);

        // Initialize TextToSpeech engine.
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                // noop
            }
        });
        tts.setLanguage(Locale.getDefault());

        setContentView(R.layout.activity_main);

        // Initialize the views
        textView = (TextView) findViewById(R.id.textView);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        textViewer = findViewById(R.id.textViewer);
        fab = (FloatingActionButton) findViewById(R.id.volume);
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar); // set toolbar as the ActionBar

        isSearchOpen = false; // what is shown - search box or activity title

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tts.isSpeaking()) {
                    tts.stop();
                    fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow));
                } else {
                    fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stop));
                    tts.speak(textView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.tts.stop();
        this.queue.stop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.queue.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.tts.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        searchBtn = menu.findItem(R.id.search_btn);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search_btn)
            handleSearchButton();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(isSearchOpen)
            hideSearchBox();
        else
            super.onBackPressed();
    }

    private void search(String query) {        // fetches the article and loads it into the viewer.
        textViewer.requestFocus();
        String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + encodeURIComponent(query);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {       // response received
                        progress.setVisibility(View.INVISIBLE);         // hide the progress bar
                        try {
                            JSONObject pages = response.getJSONObject("query")
                                    .getJSONObject("pages");
                            String firstPage = pages.keys().next();         // extract the first result
                            String text = pages.getJSONObject(firstPage).getString("extract");
                            setTitle(pages.getJSONObject(firstPage).getString("title"));       // set the title to the article title.
                            textViewer.setVisibility(View.VISIBLE);         // make viewer visible
                            textView.setText(text);                         // load the content into the viewer.
                        } catch (JSONException ex) {                        // response could not be parsed.
                            Toast.makeText(MainActivity.this,"Error in parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {        // no response
                        progress.setVisibility(View.INVISIBLE);
                        // todo: view image
                        Toast.makeText(MainActivity.this,"Error in getting response", Toast.LENGTH_SHORT).show();

                    }
                });


        // Add the request to the RequestQueue.
        textViewer.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);           // make the progress bar visible
        MainActivity.this.queue.add(jsObjRequest);
    }

    // Source: http://blog.rhesoft.com/2015/03/30/tutorial-android-actionbar-with-material-design-and-search-field/
    private void handleSearchButton() {
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpen){ // if search box is shown, the cross button just clears the text
            EditText searchBox = (EditText)action.getCustomView().findViewById(R.id.search_box);
            searchBox.getText().clear();

            // set focus to the search box and bring up the keyboard in case it wasn't up.
            searchBox.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);

        } else { // if search box is not shown, show the search box
            action.setDisplayShowCustomEnabled(true);   // enable a custom view in the action bar.
            action.setCustomView(R.layout.search_box);  // add the custom view (search box)
            action.setDisplayShowTitleEnabled(false);   // hide the title

            EditText searchBox = (EditText)action.getCustomView().findViewById(R.id.search_box);
            searchBox.requestFocus();

            // bring up the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);

            // a listener to perform the actual search
            searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String query = v.getText().toString().trim();   // get the query
                        hideSearchBox();    // hide the search box
                        if (tts.isSpeaking()) {     // stop the tts
                            tts.stop();
                            fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow));
                        }
                        search(query);      // search the query
                        return true;
                    }
                    return false;
                }
            });

            // a listener to hide the search box if the user clicks somewhere else.
            searchBox.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSearchBox();
                    }
                }
            });

            // change search icon to cross
            searchBtn.setIcon(getResources().getDrawable(R.drawable.ic_cancel_search));

            isSearchOpen = true;
        }
    }

    private void hideSearchBox() {                 // hides search box and shows title
        ActionBar action = getSupportActionBar();  // get the actionbar

        action.setDisplayShowCustomEnabled(false); // disable a custom view inside the actionbar
        action.setDisplayShowTitleEnabled(true);   // show the title in the action bar

        // hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);

        // change icon in the action bar to search
        searchBtn.setIcon(getResources().getDrawable(R.drawable.ic_open_search));

        isSearchOpen = false;   // search box is not shown anymore
    }

    private static String encodeURIComponent(String s)
    {
        String result = null;

        try
        {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e)
        {
            result = s;
        }

        return result;
    }

}