package ca.brocku.imajadio.app;

import android.widget.ImageView;


public class StatusBarThread extends Thread {

    public boolean shouldContinue = true;

    boolean isPlaying;
    MainActivity activity;
    ImageView imgPreview;
    float realGrainDuration;

    StatusBarThread(boolean isPlaying, MainActivity activity, ImageView imgPreview, float realGrainDuration) {
        this.isPlaying = isPlaying;
        this.activity = activity;
        this.imgPreview = imgPreview;
        this.realGrainDuration = realGrainDuration;
    }

    @Override
    public void run() {
        isPlaying = true;
        activity.onUpdateProgressBar(-1);

        for (int i = 0; i < imgPreview.getWidth() && shouldContinue; i++) {

            try {
                activity.onUpdateProgressBar(i);
                Thread.sleep((long) (realGrainDuration * 1000));


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        activity.onUpdateProgressBar(-1);

        isPlaying = true;
    }


    public void requestStop() {
        shouldContinue = false;
    }
}
