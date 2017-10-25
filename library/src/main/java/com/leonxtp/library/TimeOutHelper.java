package com.leonxtp.library;

import android.os.Handler;

/**
 * A timer util for counting the time past after we sent a package to the terminal
 * <p>
 * Created by leonxtp on 2017/9/17.
 * Modified by leonxtp on 2017/9/17
 */

public class TimeOutHelper {

    private ITimeOut listener;

    private Handler timeoutHanldler = new Handler();

    private Runnable timer = new Runnable() {

        @Override
        public void run() {
            stopTimer();
            if (listener != null) {
                listener.onTimeOut();
            }
        }
    };

    public void startTimer(ITimeOut timeoutListener, long delay) {
        listener = timeoutListener;
        timeoutHanldler.postDelayed(timer, delay);
    }

    public void stopTimer() {
        timeoutHanldler.removeCallbacksAndMessages(null);
    }

    public void unRegisterListener() {
        listener = null;
    }

    public interface ITimeOut {
        void onTimeOut();
    }

}
