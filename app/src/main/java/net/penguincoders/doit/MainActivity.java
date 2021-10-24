package net.penguincoders.doit;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.penguincoders.doit.adapters.ToDoAdapter;
import net.penguincoders.doit.models.ToDoModel;
import net.penguincoders.doit.retrofit.NetworkService;
import net.penguincoders.doit.retrofit.Product;
import net.penguincoders.doit.retrofit.myRequest;
import net.penguincoders.doit.retrofit.myResponse;
import net.penguincoders.doit.utils.DatabaseHandler;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements DialogCloseListener {
    private DatabaseHandler db;

    private RecyclerView tasksRecyclerView;
    private ToDoAdapter tasksAdapter;
    private FloatingActionButton fab;

    private List<ToDoModel> taskList;

    private FloatingActionButton fab2;
    final int LAUNCH_SECOND_ACTIVITY = 1;
    private Dialog mProgressDialog = null;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static final String ALLOW_KEY = "ALLOWED";
    public static final String CAMERA_PREF = "camera_pref";

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
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (getFromPref(MainActivity.this, ALLOW_KEY)) {
                        showSettingsAlert();
                    } else if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.CAMERA)) {
                            showAlert();
                        } else {
                            // No explanation needed, we can request the permission.
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    MY_PERMISSIONS_REQUEST_CAMERA);
                        }
                    }
                } else {
                    if (hasConnection(getApplicationContext())) {
                        startActivityForResult(new Intent(getApplicationContext(), ScanCodeActivity.class), LAUNCH_SECOND_ACTIVITY);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Необходимо подключение к Интернету",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Activity Data Transmission on QR Scanner
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        showCustomProgressDialog();
        if (requestCode == LAUNCH_SECOND_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                String qr_result = data.getStringExtra("qr_result");
                qr_result = "http://consumer.oofd.kz/?i=1734974179&f=010100465991&s=13507.0&t=20210922T224125";

                NetworkService.getInstance()
                        .getBasketApi()
                        .getBasket(new myRequest(qr_result))
                        .enqueue(new Callback<myResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<myResponse> call, @NonNull Response<myResponse> response) {
                                myResponse basket = response.body();

                                TextView tvFinalSum = findViewById(R.id.finalSum);

                                db = new DatabaseHandler(getApplicationContext());
                                db.openDatabase();

                                List<ToDoModel> taskList = db.getAllTasks();
                                for (ToDoModel element : taskList) {
                                    if (element.getStatus() == 1) {
                                        db.deleteTask(element.getId());
                                    }
                                }

                                double finalSum = 0;

                                for (Product product : basket.getBasket()) {
                                    int id = db.getIdByTask(product.getName());

                                    String text = "";
                                    text += "цена: " + product.getPrice() + " тг" + "\n";
                                    text += "количество: " + product.getQuantity() + " " + product.getUnit() + "\n";
                                    text += "итого: " + product.getSum() + " тг";
                                    finalSum += product.getSum();

                                    if (id != -1) {
                                        db.updateStatus(id,1);
                                        db.updateExtra(id, text);
                                    } else {
                                        db.insertTask(new ToDoModel(1, product.getName(), text));
                                    }
                                }
                                tvFinalSum.setText("Итого: " + finalSum + " тг");

                                taskList = db.getAllTasks();
                                Collections.reverse(taskList);
                                tasksAdapter.setTasks(taskList);
                                tasksAdapter.notifyDataSetChanged();
                                hideProgressDialog();
                            }

                            @Override
                            public void onFailure(@NonNull Call<myResponse> call, @NonNull Throwable t) {
                                Log.e("ERROR", t.getMessage());
                                t.printStackTrace();
                                hideProgressDialog();
                            }
                        });
                // END
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                hideProgressDialog();
                //Toast.makeText(this, "Отмена", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Progress Dialog
    private void showCustomProgressDialog() {
        mProgressDialog = new Dialog(MainActivity.this);
        mProgressDialog.setContentView(R.layout.dialog_custom_progress);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    // -------------------------------------------------------------------------
    // Dialog on Action (Add, Update, Delete)
    @Override
    public void handleDialogClose(DialogInterface dialog) {
        taskList = db.getAllTasks();
        Collections.reverse(taskList);
        tasksAdapter.setTasks(taskList);
        tasksAdapter.notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // Camera Permission
    public static void saveToPreferences(MainActivity context, String key,
                                         Boolean allowed) {
        SharedPreferences myPrefs = context.getSharedPreferences
                (CAMERA_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(key, allowed);
        prefsEditor.commit();
    }

    public static Boolean getFromPref(Context context, String key) {
        SharedPreferences myPrefs = context.getSharedPreferences
                (CAMERA_PREF, Context.MODE_PRIVATE);
        return (myPrefs.getBoolean(key, false));
    }

    private void showAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Сообщение");
        alertDialog.setMessage("Необходимо разрешение на использование Камеры");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Отклонить",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //finish();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Разрешить",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);

                    }
                });
        alertDialog.show();
    }

    private void showSettingsAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Сообщение");
        alertDialog.setMessage("Приложению необходимо разрешение на использование Камеры");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Отклонить",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //finish();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Настройки",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startInstalledAppDetailsActivity(MainActivity.this);
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale =
                                ActivityCompat.shouldShowRequestPermissionRationale
                                        (this, permission);
                        if (showRationale) {
                            showAlert();
                        } else if (!showRationale) {
                            // user denied flagging NEVER ASK AGAIN
                            // you can either enable some fall back,
                            // disable features of your app
                            // or open another dialog explaining
                            // again the permission and directing to
                            // the app setting
                            saveToPreferences(MainActivity.this, ALLOW_KEY, true);
                        }
                    }
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request

        }
    }

    public static void startInstalledAppDetailsActivity(final Activity context) {
        if (context == null) {
            return;
        }
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    // -------------------------------------------------------------------------
    // Internet Connection Check
    public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        return false;
    }
}



