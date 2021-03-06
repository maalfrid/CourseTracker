package gruppe087.coursetracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.InterpolatorRes;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;


public class ChooseCourseAtSetupActivity extends AppCompatActivity {

    SelectListAdapter<String> selectListAdapter;
    ArrayList<String> listItems;
    ListView lv;
    EditText et;
    HttpGetRequest getRequest;
    ArrayList<String> overview_list;
    HashSet<Position> selected = new HashSet<Position>();
    LoginDataBaseAdapter loginDataBaseAdapter;
    CourseAdapter courseAdapter;
    UserCourseAdapter userCourseAdapter;
    ArrayList<String> infoList;
    public static final String PREFS_NAME = "CTPrefs";
    SharedPreferences settings;
    String username;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        username = settings.getString("username", "default");

        // get Instance  of Database Adapter
        courseAdapter = new CourseAdapter(this);
        courseAdapter = courseAdapter.open();
        userCourseAdapter = new UserCourseAdapter(this).open();


        setContentView(R.layout.activity_choose_course_at_setup);

        lv = (ListView)findViewById(R.id.initlv);
        selectListAdapter = new SelectListAdapter<String>(this, listItems, selected, getResources());

        //START LISTVIEW

        lv = (ListView) findViewById(R.id.initlv);
        et = (EditText) findViewById(R.id.searchtxt);

        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setItemsCanFocus(false);

        initList();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String selectedFromList = (String)(selectListAdapter.getItem(position));
                int pos = overview_list.indexOf(selectedFromList);
                boolean isselected = isSelected(pos);
                if (!isSelected(pos)){
                    view.setBackgroundColor(getResources().getColor(R.color.gray));
                    select(pos);
                } else {
                    view.setBackgroundColor(getResources().getColor(R.color.white));
                    deselect(pos);
                }
                selectListAdapter.updateSelected(selected);
                selectListAdapter.notifyDataSetChanged();

            }
        });


        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals("")){
                    //reset listview
                    initList();
                    colourSelectedItems();
                }
                else {
                    //perform search
                    searchItem(s.toString());

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //END LISTVIEW




        final Button button = (Button) findViewById(R.id.dummy_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Define action on click

                ArrayList<String> courseCodes = new ArrayList<String>();
                for (Position pos : selected){
                    int i = pos.getValue();
                    String courseCode = overview_list.get(i).split(" ")[0];
                    getRequest = new HttpGetRequest("getCourse.php ");
                    String result;
                    try {
                        result = getRequest.execute("courseID",courseCode).get();
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                        result=null;
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        result=null;
                    }
                    // Parsing the result and turning it into an JSONArray, so that it is simpler to pick
                    // out the fields that are wanted.
                    String courseID = null;
                    try {
                        JSONArray jsonArray = new JSONArray(result);

                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        courseID = jsonObject.getString("courseID");
                        String courseName = jsonObject.getString("courseName");
                        String location = jsonObject.getString("location");
                        String examDate = jsonObject.getString("examDate");



                        courseAdapter.insertEntry(courseID, courseName, location, examDate);
                        userCourseAdapter.insertEntry(username, courseID);



                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (SQLiteConstraintException e) {
                        e.printStackTrace();
                        userCourseAdapter.insertEntry(username, courseID);

                    }

                }


                //Intent myIntent = new Intent(RegisterNameActivity.this, TodayOverviewActivity.class);
                Intent myIntent = new Intent(ChooseCourseAtSetupActivity.this, MainActivity.class);
                //Optional parameters: myIntent.putExtra("key", value);
                ChooseCourseAtSetupActivity.this.startActivity(myIntent);

                // Transition
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

    }

    private void colourSelectedItems(){
        for (int i = 0; i < selectListAdapter.getCount(); i++){
            View v = getViewByPosition(i,lv);
            String s = (String) selectListAdapter.getItem(i);
            int index = overview_list.indexOf(s);
            if (selected.contains(index)) {
                v.setBackgroundColor(getResources().getColor(R.color.gray));
            } else {
                v.setBackgroundColor(getResources().getColor(R.color.white));
            }

        }
    }

    public void searchItem(String textToSearch){

        if(overview_list.size() != 0) {
            for (int i = 0; i < overview_list.size(); i++) {

                String item = overview_list.get(i);

                if (!item.toLowerCase().contains(textToSearch.toLowerCase())) {
                    selectListAdapter.addHiddenPosition(i);
                } else {
                    selectListAdapter.removeHiddenPosition(i);
                }
            }
        }
        colourSelectedItems();

        ArrayList sortedList = new ArrayList(selected);
        Collections.sort(sortedList);
        selectListAdapter.notifyDataSetChanged();

    }

    public void initList(){

        listItems = new ArrayList<String>();
        // Initializing getRequest class
        getRequest = new HttpGetRequest("addCoursesSetup.php");
        String result;
        try {
            result = getRequest.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            result=null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            result=null;
        }


        String[] overview = new String[]{};
        overview_list = new ArrayList<String>(Arrays.asList(overview));
        infoList = new ArrayList<String>();

        // Parsing the result and turning it into an JSONArray, so that it is simpler to pick
        // out the fields that are wanted.
        try {
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i<jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String courseID = jsonObject.getString("courseID");
                String courseName = jsonObject.getString("courseName");
                //String location = jsonObject.getString("location");
                //String examDate = jsonObject.getString("examDate");

                overview_list.add(courseID + " " + courseName);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Create a List from String Array elements
        selectListAdapter = new SelectListAdapter<String>
                (this, overview_list, selected, getResources());


        // DataBind ListView with items from SelectListAdapter
        lv.setAdapter(selectListAdapter);
        selectListAdapter.notifyDataSetChanged();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void select(int pos){
        selected.add(new Position(pos));
    }

    public void deselect (int pos){
        selected.remove(new Position(pos));
    }

    public boolean isSelected(int pos){
        Position s = new Position(pos);
        if(selected.contains(s)){
            return true;
        } else {
            return false;
        }
    }
    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

}

