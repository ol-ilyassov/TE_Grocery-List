package net.penguincoders.doit;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.penguincoders.doit.adapters.ToDoAdapter;
import net.penguincoders.doit.models.ToDoModel;
import net.penguincoders.doit.utils.DatabaseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity implements DialogCloseListener {
    private DatabaseHandler db;

    private RecyclerView tasksRecyclerView;
    private ToDoAdapter tasksAdapter;
    private FloatingActionButton fab;

    // Main
    private FloatingActionButton fab2;
    final int LAUNCH_SECOND_ACTIVITY = 1;
    private TextView test;

    private List<ToDoModel> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        db = new DatabaseHandler(this);
        db.openDatabase();

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksAdapter = new ToDoAdapter(db, MainActivity.this);
        tasksRecyclerView.setAdapter(tasksAdapter);

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new RecyclerItemTouchHelper(tasksAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);

        fab = findViewById(R.id.fab);

        taskList = db.getAllTasks();
        Collections.reverse(taskList);

        tasksAdapter.setTasks(taskList);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);
            }
        });

        fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getApplicationContext(), ScanCodeActivity.class), LAUNCH_SECOND_ACTIVITY);
            }
        });

        Log.i("INFO", "START!");

        // START

        test = findViewById(R.id.test);

        // END
    }

    @Override
    public void handleDialogClose(DialogInterface dialog) {
        taskList = db.getAllTasks();
        Collections.reverse(taskList);
        tasksAdapter.setTasks(taskList);
        tasksAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCH_SECOND_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                // START
                String result = data.getStringExtra("qr_result");
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();

                // START

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://185.22.64.115:6001/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                JsonPlaceHolderApi jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

                Call<ResponseBasket> call = jsonPlaceHolderApi.getBasket(
                        //result
                        "http://consumer.oofd.kz/?i=1734974179&f=010100465991&s=13507.0&t=20210922T224125"
                );

                call.enqueue(new Callback<ResponseBasket>() {
                    @Override
                    public void onResponse(Response<ResponseBasket> response, Retrofit retrofit) {
                        if (!response.isSuccess()) {
                            test.setText("Code: " + response.code());
                            return;
                        }

                        ResponseBasket responseBasket = response.body();
                        Basket basket = responseBasket.getBasket();

                        StringBuilder content = new StringBuilder();
                        for (Product element: basket.getProductList()) {
                            content.append(element.getName()).append(" ");
                        }
                        String result = content.toString();
                        test.setText(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        test.setText(t.getMessage());
                    }
                });

                // END
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "No Result", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String getApi() throws IOException, JSONException {
        URL url = new URL("http://185.22.64.115:6001/getBasket");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        //String jsonInputString = "{\"url\": \"" + result + "\"}";
        String jsonInputString = "{\"url\": \"http://consumer.oofd.kz/?i=1734974179&f=010100465991&s=13507.0&t=20210922T224125\"}";

        //Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String jsonOutput;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            jsonOutput = response.toString();
            System.out.println(response);
        }

        JSONObject obj = new JSONObject(jsonOutput);
        JSONArray basket = obj.getJSONArray("basket");

        //Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < basket.length(); i++) {
            String name = basket.getJSONObject(i).getString("name");
            //System.out.println(name);

        }
        return "";
    }
}



