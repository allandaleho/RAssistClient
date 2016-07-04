package com.ramco.rassistclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBPrivateChatManager;
import com.quickblox.chat.QBSignaling;
import com.quickblox.chat.QBWebRTCSignaling;
import com.quickblox.chat.listeners.QBVideoChatSignalingManagerListener;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBSettings;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.AppRTCAudioManager;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCConfig;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBSignalingSpec;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientSessionCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSignalingCallback;
import com.quickblox.videochat.webrtc.exception.QBRTCException;
import com.quickblox.videochat.webrtc.exception.QBRTCSignalException;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;
import com.quickblox.videochat.webrtc.view.RTCGLVideoView;
import com.ramco.rassistclient.util.Consts;
import com.ramco.rassistclient.util.Conversation;
import com.ramco.rassistclient.util.OnCallEventsController;
import com.ramco.rassistclient.view.DrawingView;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;

import java.util.HashMap;
import java.util.Map;

public class MainConfActivity extends Activity implements QBRTCClientSessionCallbacks, QBRTCSessionConnectionCallbacks, QBRTCSignalingCallback, QBRTCClientVideoTracksCallbacks,
        OnCallEventsController {
    private static final String TAG = MainConfActivity.class.getSimpleName();
    private static Context context;

    private static QBChatService chatService;
    private QBRTCClient rtcClient;
    private QBRTCSessionUserCallback sessionUserCallback;
    private AppRTCAudioManager audioManager;
    private QBRTCSession currentSession;
    private Conversation conv;

    private static String login = "Client1";
    private static String password = "testtest";
    private static int id = 12564924;//12591115

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_conf);

        drawingView = (DrawingView) findViewById(R.id.drawing);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // Initialize QuickBlox application with credentials.
        //
        QBSettings.getInstance().init(getApplicationContext(), Consts.APP_ID, Consts.AUTH_KEY, Consts.AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(Consts.ACCOUNT_KEY);

        QBChatService.setDebugEnabled(true);
        //QBChatService.setDefaultAutoSendPresenceInterval(60); //seconds
        //chatService = QBChatService.getInstance();

        createSession();

    }

    @Override
    public void onStop() {
        super.onStop();

        if (conv != null) {
            conv.stopAcceptCall();
        }
    }

    @Override
    public void onBackPressed() {
        if (getCurrentSession() != null) {
            getCurrentSession().hangUp(new HashMap<String, String>());
        }

        this.finish();
    }

    public void hangup(View view) {
        if (getCurrentSession() != null) {
            getCurrentSession().hangUp(new HashMap<String, String>());
        }
        startActivity(new Intent(MainConfActivity.this, LoginActivity.class));
    }

    private void createSession() {
        final QBUser user = new QBUser(login, password);
        context = this.getApplicationContext();

        // CREATE SESSION WITH USER
        // If you use create session with user data,
        // then the user will be logged in automatically
        QBAuth.createSession(login, password, new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession session, Bundle bundle) {

                user.setId(session.getUserId());

                // INIT CHAT SERVICE
                chatService = QBChatService.getInstance();

                // LOG IN CHAT SERVICE
                chatService.login(user, new QBEntityCallback<Void>() {

                    @Override
                    public void onSuccess(Void result, Bundle bundle) {
                        Log.d(TAG, "onSuccess login to chat");
                        startCallListen();
                    }

                    @Override
                    public void onError(QBResponseException errors) {
                        Log.d(TAG, "ERROR : " + errors.getStackTrace());
                        Toast.makeText(context, errors.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(QBResponseException errors) {
                //error
            }
        });
    }

    /**
     * Main function to initiate call listen process.
     */
    private void startCallListen() {
        initQBRTCClient();
        initAudioManager();
        //initVideoTrack();
    }

    private void initQBRTCClient() {
        rtcClient = QBRTCClient.getInstance(this);

        QBChatService.getInstance().getVideoChatWebRTCSignalingManager()
                .addSignalingManagerListener(new QBVideoChatSignalingManagerListener() {
                    @Override
                    public void signalingCreated(QBSignaling qbSignaling, boolean createdLocally) {
                        if (!createdLocally) {
                            rtcClient.addSignaling((QBWebRTCSignaling) qbSignaling);
                        }
                    }
                });

        rtcClient.setCameraErrorHendler(new VideoCapturerAndroid.CameraErrorHandler() {
            @Override
            public void onCameraError(final String s) {
                MainConfActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });


        // Configure
        //
        QBRTCConfig.setMaxOpponentsCount(6);
        QBRTCConfig.setDisconnectTime(30);
        QBRTCConfig.setAnswerTimeInterval(30l);
        QBRTCConfig.setDebugEnabled(true);


        // Add activity as callback to RTCClient
        rtcClient.addSessionCallbacksListener(this);
        // Start mange QBRTCSessions according to VideoCall parser's callbacks
        rtcClient.prepareToProcessCalls();

        QBChatService.getInstance().addConnectionListener(new AbstractConnectionListener() {

            @Override
            public void connectionClosedOnError(Exception e) {
                //showNotificationPopUp(R.string.connection_was_lost, true);
                Log.d(TAG, "WARN : Connection was lost1");
                Toast.makeText(context, "Connection was lost", Toast.LENGTH_LONG).show();
            }

            @Override
            public void reconnectionSuccessful() {
                //showNotificationPopUp(R.string.connection_was_lost, false);
                Log.d(TAG, "Reconnection was successful");
                Toast.makeText(context, "Reconnection was successful", Toast.LENGTH_LONG).show();
            }

            @Override
            public void reconnectingIn(int seconds) {
                Log.i(TAG, "reconnectingIn " + seconds);
            }
        });
    }

    private void initAudioManager() {
        audioManager = AppRTCAudioManager.create(this, new AppRTCAudioManager.OnAudioManagerStateListener() {
            @Override
            public void onAudioChangedState(AppRTCAudioManager.AudioDevice audioDevice) {
                Toast.makeText(context, "Audio device swicthed to  " + audioDevice, Toast.LENGTH_LONG).show();
            }
        });
        audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
        audioManager.setOnWiredHeadsetStateListener(new AppRTCAudioManager.OnWiredHeadsetStateListener() {
            @Override
            public void onWiredHeadsetStateChanged(boolean plugged, boolean hasMicrophone) {
                Toast.makeText(context, "Headset " + (plugged ? "plugged" : "unplugged"), Toast.LENGTH_LONG).show();
//                if (getCurrentFragment() instanceof ConversationFragment) {
//                    ((ConversationFragment) getCurrentFragment()).enableDinamicToggle(plugged);
//                }
            }
        });
    }

    private void initVideoTrack() {
        currentSession.addVideoTrackCallbacksListener(this);
    }

    /**
     * Called each time when new session request is received.
     */
    @Override
    public void onReceiveNewSession(final QBRTCSession session) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "Session " + session.getSessionID() + " are incoming");
                String curSession = (getCurrentSession() == null) ? null : getCurrentSession().getSessionID();

                if (getCurrentSession() == null) {
                    Log.d(TAG, "Start new session");
                    initCurrentSession(session);
                    audioManager.init();
                    conv = new Conversation();
                    conv.startAcceptCall(MainConfActivity.this);
                    //session.acceptCall(session.getUserInfo());
                    Log.d(TAG, "Call accepted");
//                    addIncomeCallFragment(session);
//
//                    isInCommingCall = true;
//                    initIncommingCallTask();
                } else {
                    Log.d(TAG, "Stop new session. Device now is busy");
                    session.rejectCall(null);
                }

            }
        });
    }

    /**
     * Called in case when user didn't answer in timer expiration period
     */
    @Override
    public void onUserNotAnswer(QBRTCSession session, Integer userID) {

    }

    /**
     * Called in case when opponent has rejected you call
     */
    @Override
    public void onCallRejectByUser(QBRTCSession session, Integer userID, Map<String, String> userInfo) {

    }

    /**
     * Called in case when opponent has accepted you call
     */
    @Override
    public void onCallAcceptByUser(QBRTCSession session, Integer userID, Map<String, String> userInfo) {

    }

    /**
     * Called in case when user didn't make any actions on received session
     */
    @Override
    public void onUserNoActions(QBRTCSession session, Integer userID) {

    }

    /**
     * Called in case when session will close
     */
    @Override
    public void onSessionStartClose(QBRTCSession session) {

    }

    /**
     * Called when session is closed.
     */
    @Override
    public void onSessionClosed(QBRTCSession session) {
        if (audioManager != null) {
            audioManager.close();
        }
        releaseCurrentSession();
    }

    @Override
    public void onReceiveHangUpFromUser(final QBRTCSession session, final Integer userID, Map<String, String> map) {
        if (session.equals(getCurrentSession())) {

            if (sessionUserCallback != null) {
                sessionUserCallback.onReceiveHangUpFromUser(session, userID);
            }

            //final String participantName = DataHolder.getUserNameByID(userID);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //showToast("User " + participantName + " " + getString(R.string.hungUp) + " conversation");
                    Log.d(TAG, "Receive hang up");
                    Toast.makeText(context, "Receive hang up", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onStartConnectToUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onConnectedToUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onConnectionClosedForUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onDisconnectedFromUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onDisconnectedTimeoutFromUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onConnectionFailedWithUser(QBRTCSession qbrtcSession, Integer integer) {

    }

    @Override
    public void onError(QBRTCSession qbrtcSession, QBRTCException e) {

    }

    @Override
    public void onSuccessSendingPacket(QBSignalingSpec.QBSignalCMD qbSignalCMD, Integer integer) {

    }

    @Override
    public void onErrorSendingPacket(QBSignalingSpec.QBSignalCMD qbSignalCMD, Integer integer, QBRTCSignalException e) {
        Toast.makeText(context, "Disconnected... Please check your Internet connection!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocalVideoTrackReceive(QBRTCSession qbrtcSession, QBRTCVideoTrack qbrtcVideoTrack) {

    }

    @Override
    public void onRemoteVideoTrackReceive(QBRTCSession qbrtcSession, QBRTCVideoTrack qbrtcVideoTrack, Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);

//        RTCGLVideoView remoteVideoView = (RTCGLVideoView) findViewById(R.id.opponentView);
//        if (remoteVideoView != null) {
//            fillVideoView(remoteVideoView, qbrtcVideoTrack, true);
//        }
    }

    @Override
    public void onSwitchAudio() {
        if (audioManager.getSelectedAudioDevice() == AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                || audioManager.getSelectedAudioDevice() == AppRTCAudioManager.AudioDevice.EARPIECE) {
            audioManager.setAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        } else {
            audioManager.setAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
        }
    }

    @Override
    public void onUseHeadSet(boolean use) {
        audioManager.setManageHeadsetByDefault(use);
    }

    public interface QBRTCSessionUserCallback {
        void onUserNotAnswer(QBRTCSession session, Integer userId);

        void onCallRejectByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo);

        void onCallAcceptByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo);

        void onReceiveHangUpFromUser(QBRTCSession session, Integer userId);
    }

    public QBRTCSession getCurrentSession() {
        return currentSession;
    }

    public void initCurrentSession(QBRTCSession sesion) {
        Log.d(TAG, "Init new QBRTCSession");
        this.currentSession = sesion;
        this.currentSession.addSessionCallbacksListener(MainConfActivity.this);
        this.currentSession.addSignalingCallback(MainConfActivity.this);
    }

    public void releaseCurrentSession() {
        Log.d(TAG, "Release current session");
        this.currentSession.removeSessionCallbacksListener(MainConfActivity.this);
        this.currentSession.removeSignalingCallback(MainConfActivity.this);
        this.currentSession = null;
    }

    private void fillVideoView(RTCGLVideoView videoView, QBRTCVideoTrack videoTrack, boolean remoteRenderer) {
        videoTrack.addRenderer(new VideoRenderer(remoteRenderer ?
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.MAIN) :
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.SECOND)));
    }

    public void addTCClientConnectionCallback(QBRTCSessionConnectionCallbacks clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.addSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    public void addRTCSessionUserCallback(QBRTCSessionUserCallback sessionUserCallback) {
        this.sessionUserCallback = sessionUserCallback;
    }

    public void addVideoTrackCallbacksListener(QBRTCClientVideoTracksCallbacks videoTracksCallbacks) {
        if (currentSession != null) {
            currentSession.addVideoTrackCallbacksListener(videoTracksCallbacks);
        }
    }

    public void removeRTCClientConnectionCallback(QBRTCSessionConnectionCallbacks clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.removeSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    public void removeRTCSessionUserCallback(QBRTCSessionUserCallback sessionUserCallback) {
        this.sessionUserCallback = null;
    }

    public QBPrivateChatManager getPrivateChatManager() {
        return chatService.getPrivateChatManager();
    }

    public void startDraw(final String coord) {
        if (coord.indexOf("x:") > -1) {
            Log.d(TAG, "startDraw()");
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "staring...");
                    drawingView.startDraw(coord);
                }
            });

        } else {
            Log.d(TAG, "dont start draw");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //startDraw("x:100.0,y:407.80078;x:100.34586,y:408.38443;x:101.11733,y:408.02466;x:101.90307,y:407.24976;x:102.5,y:404.9924;x:102.5,y:401.59503;x:102.5,y:398.1328;x:102.80633,y:392.9357;x:106.03675,y:381.71844;x:119.572624,y:356.0605;x:138.32835,y:329.38004;x:158.13185,y:310.8292;x:175.07793,y:299.18353;x:188.89052,y:294.9097;x:200.0085,y:293.63025;x:210.89586,y:298.75925;x:223.55887,y:314.25833;x:236.40625,y:341.70703;x:246.29932,y:380.92746;x:255.37749,y:417.95352;x:262.39386,y:443.90283;x:268.35944,y:458.20285;x:272.31793,y:460.991;x:276.7811,y:458.7827;x:287.95737,y:445.36646;x:303.68222,y:423.9682;x:326.73392,y:394.20886;x:349.6369,y:363.92188;x:374.4711,y:335.7399;x:398.04303,y:309.6548;x:425.04868,y:282.66434;x:445.56543,y:266.5323;x:468.97546,y:250.93741;x:479.56424,y:249.84027;x:481.30038,y:251.72403;");
    }

}
