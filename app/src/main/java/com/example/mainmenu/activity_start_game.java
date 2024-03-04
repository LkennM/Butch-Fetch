package com.example.mainmenu;

import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.Locale;

public class activity_start_game extends AppCompatActivity implements pause_dialog.DialogCallback, TimerHelper.TimerCallback {

    private TextView timerCount, scoreCount;
    private boolean isMuted, isMutedSFX;
    private boolean gamePaused = false;
    private boolean isGameStarted = false;
    private boolean isPauseDialogShown = false;

    private pause_dialog pauseDialog;
    private TimerHelper timerHelper;
    private ScoreHelper scoreHelper;
    private CollisionHandler collisionHandler;
    private AnimationDrawable dogAnimation;
    private GestureDetector gestureDetector;

    private ImageView imageView, imageViewObst;
    private SimulationView simulationView;
    private ObstacleRandomizer obstacleRandomizer;
    private SoundPlayer soundPlayer;
    private Drawable coneObst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);
        isGameStarted = startGame();
        timerHelper = new TimerHelper(90000, this);
        timerHelper.startTimer();
        scoreHelper = new ScoreHelper();

        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean("isMuted", false);
        isMutedSFX = prefs.getBoolean("isMutedSfx", false);

        if (!isMuted) {
            SoundPlayer.playBGM(this);
        }

        obstacleRandomizer = new ObstacleRandomizer(this, soundPlayer);

        timerCount = findViewById(R.id.countText);
        scoreCount = findViewById(R.id.scoreText);

        Button btnPause = findViewById(R.id.btn_pause);
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!gamePaused) {
                    SoundPlayer.playSFX(isMutedSFX, 1);
                    SoundPlayer.pauseBGM();
                    pauseGame();
                    gamePaused = true;
                    pauseDialog = new pause_dialog(activity_start_game.this, activity_start_game.this);
                    pauseDialog.show();
                    btnPause.setEnabled(false);
                    isPauseDialogShown = true;
                    gestureDetector = new GestureDetector(activity_start_game.this, new MyGestureListener());
                } else {
                    gamePaused = false;
                    if (pauseDialog != null && pauseDialog.isShowing()) {
                        pauseDialog.dismiss();
                    }
                    SoundPlayer.playBGM(activity_start_game.this);
                    btnPause.setEnabled(true);
                    resumeGame();
                }
            }
        });

        pauseDialog = new pause_dialog(activity_start_game.this, activity_start_game.this);
        pauseDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!gamePaused) {
                    resumeGame();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Button btnPause = findViewById(R.id.btn_pause);
        btnPause.setEnabled(true);
    }

    private boolean startGame() {
        gestureDetector = new GestureDetector(this, new MyGestureListener());
        collisionHandler =  new CollisionHandler();

        setContentView(R.layout.activity_start_game);
        FrameLayout backgroundContainer = findViewById(R.id.backgroundContainer);
        backgroundContainer.setBackgroundResource(R.drawable.road);

        ConstraintLayout container = findViewById(R.id.container2);
        simulationView = new activity_start_game.SimulationView(this);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        container.addView(simulationView, layoutParams);

        imageView = findViewById(R.id.image);
        imageView.setBackgroundResource(R.drawable.animation);
        dogAnimation = (AnimationDrawable) imageView.getBackground();

        imageViewObst = findViewById(R.id.image2);
        imageViewObst.setBackgroundResource(R.drawable.c1);
        coneObst = imageViewObst.getBackground();

        return isGameStarted = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isGameStarted) {
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!gamePaused) {
                        updateGame();
                        simulationView.invalidate();
                        dogAnimation.start();
                    }
                    handler.postDelayed(this, 16);
                }
            });
        }
    }

    private void updateGame() {
        if (!gamePaused) {
            collisionHandler.updatePositions(getImageViewHitbox());
            if (collisionHandler.checkCollision()) {
                pauseGame();
                showGameOverDialog();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!gamePaused && !isPauseDialogShown) {
            return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
        } else {
            return true;
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        moveAnimationRight();
                    } else {
                        moveAnimationLeft();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private void moveAnimationLeft() {
        imageView.animate().translationXBy(-285).setDuration(100).start();
        collisionHandler.moveLeft();
    }

    private void moveAnimationRight() {
        imageView.animate().translationXBy(285).setDuration(100).start();
        collisionHandler.moveRight();
    }

    private Rect getImageViewHitbox() {
        int[] location = new int[2];
        imageView.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + imageView.getWidth(), location[1] + imageView.getHeight());
    }

    private class SimulationView extends View {

        public SimulationView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            Rect obstacleRect = collisionHandler.getObstacle();
            canvas.drawRect(obstacleRect, paint);

            RectF obstacleRectF = new RectF(obstacleRect);
            coneObst.setBounds((int) obstacleRectF.left, (int) obstacleRectF.top,
                    (int) obstacleRectF.right, (int) obstacleRectF.bottom);
            coneObst.draw(canvas);


        }
    }

    @Override
    public void onTimerTick(long millisUntilFinished) {
        updateScore(millisUntilFinished);
        updateCountDownText(millisUntilFinished);
    }

    @Override
    public void onTimerFinish() {
        // Code to execute when the timer finishes...
    }

    private void pauseGame() {
        timerHelper.pauseTimer();
        pauseAnimation();
        pauseCollisionHandler();
    }

    private void resumeGame() {
        gamePaused = false;
        timerHelper.startTimer();
        resumeAnimation();
        resumeCollisionHandler();
    }

    private void pauseAnimation() {
        dogAnimation.stop();
    }

    private void resumeAnimation() {
        if (!gamePaused) {
            dogAnimation.start();
        }
    }

    private void pauseCollisionHandler() {
        collisionHandler.pause();
    }

    private void resumeCollisionHandler() {
        if (!gamePaused) {
            collisionHandler.resume();
        }
    }

    private void updateCountDownText(long millisUntilFinished) {
        int seconds = (int) (millisUntilFinished / 1000);
        String timeleftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
        timerCount.setText(timeleftFormatted);
    }

    private void updateScore(long millisUntilFinished) {
        scoreHelper.updateScore(millisUntilFinished);
        int totalScore = scoreHelper.getTotalScore();
        String formattedScore = String.format("%06d", totalScore);
        scoreCount.setText(formattedScore);
    }

    private void showGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Game Over!")
                .setCancelable(false)
                .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SoundPlayer.stopBGM();
                        restartGame();
                    }
                })
                .setNegativeButton("Menu", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SoundPlayer.stopBGM();
                        finish(); // Go back to the main menu
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void restartGame() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    @Override
    public void onBooleanPassed(boolean value, boolean value2) {
        if (value) {
            resumeGame();
            SoundPlayer.playBGM(this);
        }
        SoundPlayer.muteVolume(value2);
        Button btnPause = findViewById(R.id.btn_pause);
        btnPause.setEnabled(true);
        isPauseDialogShown = false;
    }
}
