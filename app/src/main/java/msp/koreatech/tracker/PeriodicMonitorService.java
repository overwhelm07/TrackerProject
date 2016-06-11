package msp.koreatech.tracker;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class PeriodicMonitorService extends Service {
    private static final String LOGTAG = "HS_Location_Tracking";
    private static final String BROADCAST_ACTION_ACTIVITY = "msp.tracker";
    private static final String BROADCAST_ACTION_ALARM = "msp.alarm";
    private static final String BROADCAST_ACTION_LIVESTEP = "msp.tracker.step";
    AlarmManager am;
    PendingIntent pendingIntent;

    private PowerManager.WakeLock wakeLock;
    private CountDownTimer timer;

    private StepMonitor accelMonitor;
    private long period = 5000;
    private static final long activeTime = 1000;
    private static final long periodForMoving = 1000;//1초
    private static final long periodIncrement = 5000;//정지시 주기 5초 증가
    private static final long periodMax = 10000;//최대 max 20

    private LocationManager mLocationManager = null;
    private final int MIN_TIME_UPDATES = 5000; // milliseconds
    private final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // m
    private boolean isRequestRegistered = false;

    private long stepCount = 0, stepCountTV = 0;
    private int chkSecCount = 0, chkSecCount2 = 0 ;
    private int secCount = 0, secCount2 = 0;//secCount는 이동할때의 초카운트 secCount2는 정지할때 초카운트
    private boolean keepMoving = false, keepStop = false;
    private boolean isEnd = false, isSetTime = false, isSetTime2 = false;
    ListViewItem info;


    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 그리고 다시 동일 시간 후 alarm이 발생하도록 설정한다.
    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BROADCAST_ACTION_ALARM)) {
                Log.d(LOGTAG, "Alarm fired!!!!");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //-----------------

                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HS_Wakelock");
                // ACQUIRE a wakelock here to collect and process accelerometer data and control location updates
                wakeLock.acquire();

                accelMonitor = new StepMonitor(context);
                accelMonitor.onStart();

                timer = new CountDownTimer(activeTime, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        Log.d(LOGTAG, "1-second accel data collected!!");
                        // stop the accel data update
                        accelMonitor.onStop();

                        boolean moving = accelMonitor.isMoving();
                        // 움직임 여부에 따라 GPS location update 요청 처리
                        if(moving) {//이동
                            isSetTime2 = false;
                            //정지중에 이동 되었을 때
                            if(keepStop && secCount2 < 300){
                                //isSetTime = true;
                                info.setEndTime();//정지끝나는시간
                                Log.i("정지끝나는시간", info.setEndTime());
                                info.setIsMoving(false);
                                info.setStepCount(0);
                                info.setLocation("location");
                                //keepStop = false;
                                isEnd = true;
                            }else{
                                if(keepStop){
                                    secCount2 = 0;
                                    //chkSecCount2 = 0;
                                }else if(!keepStop){
                                    secCount2 = 0;
                                }
                            }


                            stepCount++;
                            Log.e("stepCount", String.valueOf(stepCount));
                            //안움직이고 있다가 움직임이 감자되면 시작시간을 Set
                            if(!keepMoving && !keepStop && !isSetTime){
                                isSetTime = true;
                                info.setStartTime();//이동시작시간
                                Log.i("이동시작시간", info.getStartTime());
                            }
                            if(secCount >= 10){//1초당 검사해서 움직이면 증가 60되면 1분이니깐 화면에 표시
                                keepMoving = true;
                                //movingTV.setText("Moving");
                            }else{
                                secCount++;
                            }

                        } else {//정지
                            isSetTime = false;
                            //secCount2>=300 GPS센싱-->실외 장소

                            //이동중에 정지가 되었을 때
                            if(keepMoving && secCount < 10){
                                //isSetTime2 = true;
                                info.setEndTime();//이동 끝나는 시간
                                Log.i("이동 끝나는 시간", info.setEndTime());
                                info.setIsMoving(true);//움직임이 1분동안 있었으니깐 이동으로 표시하기위해 true
                                info.setStepCount(stepCountTV);
                                info.setLocation("");
                                stepCount = 0;
                                //keepMoving = false;
                                isEnd = true;
                            }else{
                                stepCountTV = stepCount;
                                stepCount = 0;
                                secCount = 0;
                                //정지가 되었을때 0 -> 1이상 되었을때 secCount를 초기화한다.
                                //if(keepMoving && chkSecCount++ >= 1){
                                /*if(keepMoving){
                                    stepCount = 0;
                                    secCount = 0;
                                    chkSecCount = 0;
                                }else if(!keepMoving){
                                    stepCount = 0;
                                    secCount = 0;
                                }*/
                            }

                            //이동중이지 않을때 정지되어 있으면 시작시간을 Set
                            if(!keepStop && !keepMoving && !isSetTime2){
                                isSetTime2 = true;
                                info.setStartTime();//정지 시작시간
                                Log.i("정지시작시간", info.getStartTime());
                            }
                            if(secCount2 >= 30){//정지를 300초이상(5분)이상되면 화면에 표시
                                keepStop = true;
                            }else{
                                Log.d("secCount2 : ", String.valueOf(secCount2));
                                secCount2+=period/1000;//정지되어있을때는 1초마다 호출이 아닌 주기마다 호출이되기 때문에 현재 주기(millisecond)에 1000을 나눠 현재 주기의 초를 더함
                            }
                        }
                        Log.e("secCount", String.valueOf(secCount));
                        Intent intent = new Intent(BROADCAST_ACTION_LIVESTEP);
                        intent.putExtra("step", stepCount);
                        sendBroadcast(intent);
                        // 움직임 여부에 따라 다음 alarm 설정
                        setNextAlarm(moving);

                        // When you finish your job, RELEASE the wakelock
                        wakeLock.release();
                    }
                };
                timer.start();
            }
        }
    };



    private void setNextAlarm(boolean moving) {

        // 움직임이면 3초 period로 등록
        // 움직임이 아니면 5초 증가, max 20초로 제한
        if(moving) {
            Log.d(LOGTAG, "MOVING!!");
            period = periodForMoving;
        } else {
            Log.d(LOGTAG, "NOT MOVING!!");
            period = period + periodIncrement;
            if(period >= periodMax) {
                period = periodMax;
            }
        }
        Log.d(LOGTAG, "Next alarm: " + period);

        // 다음 alarm 등록
        Intent in = new Intent(BROADCAST_ACTION_ALARM);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period - activeTime, pendingIntent);

        //*****
        // 화면에 정보 표시를 위해 activity의 broadcast receiver가 받을 수 있도록 broadcast 전송
        if(isEnd){
            isEnd = false;
            //isSetTime = false;
            //isSetTime2 = false;
            keepMoving = false;
            keepStop = false;
            Intent intent = new Intent(BROADCAST_ACTION_ACTIVITY);
            intent.putExtra("info", info);
            // broadcast 전송
            sendBroadcast(intent);

        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.d(LOGTAG, "onCreate");

        // Alarm 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION_ALARM);
        registerReceiver(AlarmReceiver, intentFilter);

        // AlarmManager 객체 얻기
        am = (AlarmManager)getSystemService(ALARM_SERVICE);

        info = new ListViewItem();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Log.d(LOGTAG, "onStartCommand");
        Toast.makeText(this, "Activity Monitor 시작", Toast.LENGTH_SHORT).show();

        // Alarm이 발생할 시간이 되었을 때, 안드로이드 시스템에 전송을 요청할 broadcast를 지정
        Intent in = new Intent(BROADCAST_ACTION_ALARM);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        // 설정한 시간 (5000-> 5초, 10000->10초) 후 alarm 발생
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Toast.makeText(this, "Activity Monitor 중지", Toast.LENGTH_SHORT).show();

        try {
            // Alarm 발생 시 전송되는 broadcast 수신 receiver를 해제
            unregisterReceiver(AlarmReceiver);
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        // AlarmManager에 등록한 alarm 취소
        am.cancel(pendingIntent);

        // release all the resources you use
        if(timer != null)
            timer.cancel();
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }
}
