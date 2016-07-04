package com.ramco.rassistclient.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.quickblox.chat.QBPrivateChat;
import com.quickblox.chat.QBPrivateChatManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBMessageListener;
import com.quickblox.chat.listeners.QBPrivateChatManagerListener;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.videochat.webrtc.QBMediaStreamManager;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.exception.QBRTCException;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;
import com.quickblox.videochat.webrtc.view.RTCGLVideoView;
import com.ramco.rassistclient.MainConfActivity;

import org.webrtc.VideoRenderer;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by 12041 on 5/12/2016.
 */
public class Conversation implements Serializable, QBRTCClientVideoTracksCallbacks, QBRTCSessionConnectionCallbacks, MainConfActivity.QBRTCSessionUserCallback {

    private String TAG = Conversation.class.getSimpleName();

    private MainConfActivity activity;
    private AudioStreamReceiver audioStreamReceiver;
    private IntentFilter intentFilter;
    private QBPrivateChatManager privateChatManager;
    private QBPrivateChatManagerListener privateChatManagerListener;

    public void startAcceptCall(MainConfActivity activity) {
        this.activity = activity;

        intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);

        audioStreamReceiver = new AudioStreamReceiver();

        activity.registerReceiver(audioStreamReceiver, intentFilter);

        initVideoTrack();

        QBRTCSession session = activity.getCurrentSession();
        session.acceptCall(session.getUserInfo());

        activity.addTCClientConnectionCallback(this);
        activity.addRTCSessionUserCallback(this);

        privateChatManagerListener = new QBPrivateChatManagerListener() {
            @Override
            public void chatCreated(final QBPrivateChat privateChat, final boolean createdLocally) {
                if(!createdLocally){
                    privateChat.addMessageListener(privateChatMessageListener);
                }
            }
        };
        activity.getPrivateChatManager().addPrivateChatManagerListener(privateChatManagerListener);
    }

    public void stopAcceptCall() {
        activity.unregisterReceiver(audioStreamReceiver);
        activity.removeRTCClientConnectionCallback(this);
        activity.removeRTCSessionUserCallback(this);
    }

    private void initVideoTrack() {
        activity.addVideoTrackCallbacksListener(this);
    }

    private class AudioStreamReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(AudioManager.ACTION_HEADSET_PLUG)) {
                Log.d(TAG, "ACTION_HEADSET_PLUG " + intent.getIntExtra("state", -1));
            } else if (intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                Log.d(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED " + intent.getIntExtra("EXTRA_SCO_AUDIO_STATE", -2));
            }

            //dynamicToggleVideoCall.setChecked(intent.getIntExtra("state", -1) == 1);

        }
    }

//    private void initChat(int opponentId) {
//        privateChatManager = activity.getPrivateChatManager();
//        privateChatManager.createDialog(opponentId, new QBEntityCallback<QBDialog>() {
//            @Override
//            public void onSuccess(QBDialog dialog, Bundle args) {
//                Log.d(TAG, "initChat Success");
//            }
//
//            @Override
//            public void onError(QBResponseException errors) {
//                Log.d(TAG, "initChat Error");
//            }
//        });
//    }

    QBMessageListener<QBPrivateChat> privateChatMessageListener = new QBMessageListener<QBPrivateChat>() {
        @Override
        public void processMessage(QBPrivateChat privateChat, final QBChatMessage chatMessage) {
            Log.d(TAG, "QBMessageListener processMessage triggered");
            Log.d(TAG, "Chat : " + chatMessage.getBody());
            activity.startDraw(chatMessage.getBody());
        }

        @Override
        public void processError(QBPrivateChat privateChat, QBChatException error, QBChatMessage originMessage){
            Log.d(TAG, "QBMessageListener processError triggered");
        }
    };

    @Override
    public void onLocalVideoTrackReceive(QBRTCSession qbrtcSession, QBRTCVideoTrack qbrtcVideoTrack) {

    }

    @Override
    public void onRemoteVideoTrackReceive(QBRTCSession qbrtcSession, QBRTCVideoTrack qbrtcVideoTrack, Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);

        // allan switch camera
        QBMediaStreamManager mediaStreamManager = qbrtcSession.getMediaStreamManager();
        boolean done = mediaStreamManager.switchCameraInput(new Runnable() {
            @Override
            public void run() {
                // switch done
                Log.d(TAG, "SWITCH DONE");
            }
        });

        int currentCameraId = mediaStreamManager.getCurrentCameraId();
        // end

//        RTCGLVideoView remoteVideoView = (RTCGLVideoView) activity.findViewById(R.id.opponentView);
//        if (remoteVideoView != null) {
            //fillVideoView(remoteVideoView, qbrtcVideoTrack, true);
//        }
    }

    @Override
    public void onStartConnectToUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onStartConnectToUser called");
    }

    @Override
    public void onConnectedToUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onConnectedToUser called");
    }

    @Override
    public void onConnectionClosedForUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onConnectionClosedForUser called");
    }

    @Override
    public void onDisconnectedFromUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onDisconnectedFromUser called");
    }

    @Override
    public void onDisconnectedTimeoutFromUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onDisconnectedTimeoutFromUser called");
    }

    @Override
    public void onConnectionFailedWithUser(QBRTCSession qbrtcSession, Integer integer) {
        Log.d(TAG, "onConnectionFailedWithUser called");
    }

    @Override
    public void onError(QBRTCSession qbrtcSession, QBRTCException e) {
        Log.d(TAG, "onError called : " + e.getStackTrace());
    }

    @Override
    public void onUserNotAnswer(QBRTCSession session, Integer userId) {
        Log.d(TAG, "onUserNotAnswer called");
    }

    @Override
    public void onCallRejectByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
        Log.d(TAG, "onCallRejectByUser called");
    }

    @Override
    public void onCallAcceptByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
        Log.d(TAG, "onCallAcceptByUser called");
    }

    @Override
    public void onReceiveHangUpFromUser(QBRTCSession session, Integer userId) {
        Log.d(TAG, "onReceiveHangUpFromUser called");
    }

    private void fillVideoView(RTCGLVideoView videoView, QBRTCVideoTrack videoTrack, boolean remoteRenderer) {
        videoTrack.addRenderer(new VideoRenderer(remoteRenderer ?
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.MAIN) :
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.SECOND)));
        Log.d(TAG, (remoteRenderer ? "remote" : "local") + " Track is rendering");
    }
}
