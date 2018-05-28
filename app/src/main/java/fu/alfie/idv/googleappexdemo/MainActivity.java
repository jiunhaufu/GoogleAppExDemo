package fu.alfie.idv.googleappexdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    /*type*/
    private String oauth = "GUEST";
    /*google*/
    private static final int G_SIGN_IN = 1;
    private GoogleSignInClient mGoogleSignInClient;
    /*UI*/
    private TextView tv_name;
    private TextView tv_email;
    private TextView tv_oauthId;
    private ImageView iv_picture;
    private Button btStepCounter;
    /*fit*/
    public static final String TAG = "StepCounter";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 2;

    //    private GoogleApiClient mGoogleApiFitnessClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*UI*/
        findViews();
        /*google*/
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestId()
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initFit();
    }

    private void findViews() {
        tv_name = (TextView) findViewById(R.id.tvName);
        tv_email = (TextView) findViewById(R.id.tvEmail);
        tv_oauthId = (TextView) findViewById(R.id.tvOauthId);
        iv_picture = (ImageView) findViewById(R.id.ivPicture);

        //google登入按鈕
        Button btGoogleSignin = (Button) findViewById(R.id.btlogin);
        btGoogleSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });
        //google登出按鈕
        Button btGoogleSignout = (Button) findViewById(R.id.btlogout);
        btGoogleSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignOut();
            }
        });
        //取得google fit步數
        btStepCounter = (Button) findViewById(R.id.btstepcounter);
        btStepCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readData();
            }
        });

    }

    @Override
    protected void onStart() {
        /*google*/
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateGoogleInfo(account);
        super.onStart();
    }

    /*google*/
    private void googleSignIn() {
        if (oauth.equals("GUEST")) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, G_SIGN_IN);
        }
    }

    /*google*/
    private void googleSignOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateGoogleInfo(null);
            }
        });
    }

    private void initFit() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            accessGoogleFit();
        }
    }

    private void accessGoogleFit() {
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Successfully subscribed!");
                                } else {
                                    Log.w(TAG, "There was a problem subscribing.", task.getException());
                                }
                            }
                        });

    }

    private void readData() {
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(
                        new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                long total =
                                        dataSet.isEmpty()
                                                ? 0
                                                : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                btStepCounter.setText("My step: "+String.valueOf(total));
                                Log.i(TAG, "Total steps: " + total);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the step count.", e);
                            }
                        });
    }

//    private void accessGoogleFit() {
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        long endTime = cal.getTimeInMillis();
//        cal.add(Calendar.YEAR, -1);
//        long startTime = cal.getTimeInMillis();
//
//        mGoogleApiFitnessClient = new GoogleApiClient.Builder(this)
//                .addApi(Fitness.HISTORY_API)
//                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
//                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
//                .enableAutoManage(this, 0, (GoogleApiClient.OnConnectionFailedListener) this)
//                .build();
//
//        DataReadResult dataReadResult = Fitness.HistoryApi
//                .readData(mGoogleApiFitnessClient, readRequest)
//                .await(1, TimeUnit.MINUTES);
//
//        DataSet stepData = dataReadResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
//
//        int totalSteps = 0;
//
//        for (DataPoint dp : stepData.getDataPoints()) {
//            for(Field field : dp.getDataType().getFields()) {
//                int steps = dp.getValue(field).asInt();
//
//                totalSteps += steps;
//            }
//        }
//        Log.d("setppppppp", String.valueOf(totalSteps));
//
//    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*google*/
        if (requestCode == G_SIGN_IN) {
            //登入成功後取得資料
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                //登入成功
                updateGoogleInfo(account);
            } catch (ApiException e) {
                //登入失敗
                updateGoogleInfo(null);
            }
        }
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            accessGoogleFit();
        }
    }

    /*google*/
    private void updateGoogleInfo(GoogleSignInAccount account) {
        if (account != null) {
            String name = account.getDisplayName();
            String email = account.getEmail();
            String id = account.getId();
            String picture = String.valueOf(account.getPhotoUrl());
            if (account.getPhotoUrl() == null) {
                picture = "https://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg";
            }
            updateUI(id, name, email, picture, "GOOGLE");
        } else {
            updateUI("reset", "reset", "reset", "reset", "GOOGLE");
        }
    }

    /*UI*/
    private void updateUI(String id, String name, String email, String picture, String type) {
        if (id.equals("reset")) {
            if (oauth.equals(type)) {
                tv_oauthId.setText("id");
                tv_name.setText("name");
                tv_email.setText("email");
                iv_picture.setImageResource(R.mipmap.ic_launcher_round);
                oauth = "GUEST";
            }
        } else {
            tv_oauthId.setText(id);
            tv_name.setText(name);
            tv_email.setText(email);
            new DownloadImageTask(iv_picture).execute(picture);
            oauth = type;
        }
    }
}
