package com.example.oops.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.example.oops.DataClass.MovieDeatilsData;
import com.example.oops.R;
import com.example.oops.activity.PlayerActivity;
import com.example.oops.activity.VideoPlay;
import com.example.oops.data.database.AppDatabase;
import com.example.oops.data.database.Subtitle;
import com.example.oops.data.database.Video;
import com.example.oops.data.model.VideoSource;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class VideoPlayer {
    private final String CLASS_NAME = VideoPlayer.class.getName();
    private static final String TAG = "VideoPlayer";
    private Context context;
    private PlayerController playerController;

    private PlayerView playerView;
    private SimpleExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;
    private int widthOfScreen, index;
    private ComponentListener componentListener;
    private CacheDataSourceFactory cacheDataSourceFactory;
    private VideoSource videoSource;
    private int selectedPosition = 0;
    private boolean isLock = false;
    private AppDatabase database;

    public VideoPlayer(PlayerView playerView,
                       Context context,
                       VideoSource videoSource,
                       PlayerController mView,
                       int selectedPosition) {

        this.playerView = playerView;
        this.context = context;
        this.playerController = mView;
        this.selectedPosition = selectedPosition;
        this.videoSource = videoSource;
        this.index = videoSource.getSelectedSourceIndex();
        initializePlayer();

        database = AppDatabase.Companion.getDatabase(context);
    }


    /******************************************************************
     initialize ExoPlayer
     ******************************************************************/
    private void initializePlayer() {
        playerView.requestFocus();

        componentListener = new ComponentListener();

        cacheDataSourceFactory = new CacheDataSourceFactory(
                context,
                100 * 1024 * 1024,
                5 * 1024 * 1024);

        trackSelector = new DefaultTrackSelector(context);
//        trackSelector.setParameters(trackSelector
//                .buildUponParameters());

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(exoPlayer);
        playerView.setKeepScreenOn(true);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(componentListener);
        //build mediaSource depend on video type (Regular, HLS, DASH, etc)
        if (selectedPosition == 0) {
            mediaSource = buildMediaSource(videoSource.getVideos().get(index), cacheDataSourceFactory);
        } else {
            index = selectedPosition;
            mediaSource = buildMediaSource(videoSource.getVideos().get(index), cacheDataSourceFactory);

        }
        exoPlayer.prepare(mediaSource);
        //resume video
        seekToSelectedPosition(videoSource.getVideos().get(index).getWatchedLength(), false);

        //if (videoSource.getVideos().size() == 1 || isLastVideo()){
        //  if(MyPreference.videoPlayList.size()!=0){
        if (index == videoSource.getVideos().size() - 1) {
            playerController.disableNextButtonOnLastVideo(true);
        } else if (videoSource.getVideos().size() > 0) {
            playerController.disableNextButtonOnLastVideo(false);
        } else {
            playerController.disableNextButtonOnLastVideo(true);
        }

        if (index == 0) {
            playerController.disablePreviousButtonOnFirstVideo(true);
        } else {
            playerController.disablePreviousButtonOnFirstVideo(false);
        }

    }

    public void prepareNextVideo(MovieDeatilsData data) {
        List<Video> videoUriList = new ArrayList<>();
        videoUriList.add(new Video(data.getVideoLink(), Long.getLong("zero", 1)));
        List<Subtitle> subtitleList = new ArrayList<>();
        if (database.videoDao().getAllUrls().size() == 0) {
            database.videoDao().insertAllVideoUrl(videoUriList);
            database.videoDao().insertAllSubtitleUrl(subtitleList);
        }

        List<VideoSource.SingleVideo> singleVideos = new ArrayList<>();
        for (int i = 0; i < videoUriList.size(); i++) {

            singleVideos.add(i, new VideoSource.SingleVideo(
                    videoUriList.get(i).getVideoUrl(),
                    database.videoDao().getAllSubtitles(i + 1),
                    videoUriList.get(i).getWatchedLength())
            );

        }
        videoSource = new VideoSource(singleVideos, 0);


        mediaSource = buildMediaSource(videoSource.getVideos().get(0), cacheDataSourceFactory);
        exoPlayer.prepare(mediaSource, true, true);
        if (videoSource.getVideos().get(0).getWatchedLength() != null)
            seekToSelectedPosition(videoSource.getVideos().get(0).getWatchedLength(), false);
        if (isLastVideo())
            playerController.disableNextButtonOnLastVideo(true);
    }

    /******************************************************************
     building mediaSource depend on stream type and caching
     ******************************************************************/
    private MediaSource buildMediaSource(VideoSource.SingleVideo singleVideo, CacheDataSourceFactory cacheDataSourceFactory) {
        Uri source = Uri.parse(singleVideo.getUrl());
        @C.ContentType int type = Util.inferContentType(source);
        switch (type) {
            case C.TYPE_SS:
                Log.d(TAG, "buildMediaSource() C.TYPE_SS = [" + C.TYPE_SS + "]");
                return new SsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(source);

            case C.TYPE_DASH:
                Log.d(TAG, "buildMediaSource() C.TYPE_DASH = [" + C.TYPE_DASH + "]");
                return new DashMediaSource.Factory(cacheDataSourceFactory).createMediaSource(source);

            case C.TYPE_HLS:
                Log.d(TAG, "buildMediaSource() C.TYPE_HLS = [" + C.TYPE_HLS + "]");
                return new HlsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(source);

            case C.TYPE_OTHER:
                Log.d(TAG, "buildMediaSource() C.TYPE_OTHER = [" + C.TYPE_OTHER + "]");
                return new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(source);

            default: {
                throw new IllegalStateException("Unsupported type: " + source);
            }
        }
    }

    public void pausePlayer() {
        exoPlayer.setPlayWhenReady(false);
    }

    public void resumePlayer() {
        exoPlayer.setPlayWhenReady(true);
    }

    public void releasePlayer() {
        if (exoPlayer == null)
            return;

        playerController.setVideoWatchedLength();
        playerView.setPlayer(null);
        exoPlayer.release();
        exoPlayer.removeListener(componentListener);
        exoPlayer = null;

    }

    public SimpleExoPlayer getPlayer() {
        return exoPlayer;
    }

    public VideoSource.SingleVideo getCurrentVideo() {
        return videoSource.getVideos().get(index);
    }

    /************************************************************
     mute, unMute
     ***********************************************************/
    public void setMute(boolean mute) {
        float currentVolume = exoPlayer.getVolume();
        if (currentVolume > 0 && mute) {
            exoPlayer.setVolume(0);
            playerController.setMuteMode(true);
        } else if (!mute && currentVolume == 0) {
            exoPlayer.setVolume(1);
            playerController.setMuteMode(false);
        }
    }

    /***********************************************************
     manually select stream quality
     ***********************************************************/
    public void setSelectedQuality(Activity activity) {

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo;

        if (trackSelector != null) {
            mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {

                int rendererIndex = 0; // renderer for video
                int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
                boolean allowAdaptiveSelections =
                        rendererType == C.TRACK_TYPE_VIDEO
                                || (rendererType == C.TRACK_TYPE_AUDIO
                                && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS);


                Pair<AlertDialog, MyTrackSelectionView> dialogPair =
                        MyTrackSelectionView.getDialog(activity, trackSelector,
                                rendererIndex,
                                exoPlayer.getVideoFormat().bitrate);
                dialogPair.first.setTitle(Html.fromHtml("<font color='#FFFFFF'>Quality </font>"));
                dialogPair.second.setShowDisableOption(false);
                dialogPair.second.setAllowAdaptiveSelections(allowAdaptiveSelections);
                dialogPair.second.animate();
                Log.d(TAG, "dialogPair.first.getListView()" + dialogPair.first.getListView());
                dialogPair.first.show();
            }

        }
    }

    /***********************************************************
     double tap event and seekTo
     ***********************************************************/
    public void seekToSelectedPosition(int hour, int minute, int second) {
        long playbackPosition = (hour * 3600 + minute * 60 + second) * 1000;
        exoPlayer.seekTo(playbackPosition);
    }

    public void seekToSelectedPosition(long millisecond, boolean rewind) {
        if (rewind) {
            exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 15000);
            return;
        }
        exoPlayer.seekTo(millisecond * 1000);
    }

    public void seekToOnDoubleTap() {
        getWidthOfScreen();
        final GestureDetector gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {

                        float positionOfDoubleTapX = e.getX();

                        if (positionOfDoubleTapX < widthOfScreen / 2)
                            exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 5000);
                        else
                            exoPlayer.seekTo(exoPlayer.getCurrentPosition() + 5000);

                        Log.d(TAG, "onDoubleTap(): widthOfScreen >> " + widthOfScreen +
                                " positionOfDoubleTapX >>" + positionOfDoubleTapX);
                        return true;
                    }
                });

        playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void getWidthOfScreen() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        widthOfScreen = metrics.widthPixels;
    }

    public void seekToNext() {
        playerController.disablePreviousButtonOnFirstVideo(false);

        if (index < videoSource.getVideos().size() - 1) {
            setCurrentVideoPosition();
            index++;
            mediaSource = buildMediaSource(videoSource.getVideos().get(index), cacheDataSourceFactory);
            exoPlayer.prepare(mediaSource, true, true);
            if (videoSource.getVideos().get(index).getWatchedLength() != null)
                seekToSelectedPosition(videoSource.getVideos().get(index).getWatchedLength(), false);

            if (isLastVideo())
                playerController.disableNextButtonOnLastVideo(true);
        }
    }

    private boolean isLastVideo() {
        return index == videoSource.getVideos().size() - 1;
    }

    public void seekToPrevious() {
        playerController.disableNextButtonOnLastVideo(false);

        if (index == 1 || index == 0) {
            playerController.disablePreviousButtonOnFirstVideo(true);
        }

        if (index == 0) {
            seekToSelectedPosition(0, false);
            return;
        }

        if (index > 0) {
            setCurrentVideoPosition();
            index--;
            mediaSource = buildMediaSource(videoSource.getVideos().get(index), cacheDataSourceFactory);
            exoPlayer.prepare(mediaSource, true, true);
            if (videoSource.getVideos().get(index).getWatchedLength() != null)
                seekToSelectedPosition(videoSource.getVideos().get(index).getWatchedLength(), false);
        }
    }

    private void setCurrentVideoPosition() {
        if (getCurrentVideo() == null)
            return;
        getCurrentVideo().setWatchedLength(exoPlayer.getCurrentPosition() / 1000);//second
    }

    public int getCurrentVideoIndex() {
        return index;
    }

    public long getWatchedLength() {
        if (getCurrentVideo() == null)
            return 0;
        return exoPlayer.getCurrentPosition() / 1000;//second
    }

    /***********************************************************
     manually select subtitle
     ***********************************************************/
    public void setSelectedSubtitle(Subtitle subtitle) {

        if (TextUtils.isEmpty(subtitle.getTitle()))
            Log.d(TAG, "setSelectedSubtitle: subtitle title is empty");


        Format subtitleFormat = Format.createTextSampleFormat(
                null,
                MimeTypes.APPLICATION_SUBRIP,
                Format.NO_VALUE,
                null);

//        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
//                Util.getUserAgent(context,CLASS_NAME ));

        MediaSource subtitleSource = new SingleSampleMediaSource
                .Factory(cacheDataSourceFactory)
                .createMediaSource(Uri.parse(subtitle.getSubtitleUrl()), subtitleFormat, C.TIME_UNSET);


        //optional
        playerController.changeSubtitleBackground();

        exoPlayer.prepare(new MergingMediaSource(mediaSource, subtitleSource), false, false);
        playerController.showSubtitle(true);
        resumePlayer();
    }

    /***********************************************************
     playerView listener for lock and unlock screen
     ***********************************************************/
    public void lockScreen(boolean isLock) {
        this.isLock = isLock;
    }

    public boolean isLock() {
        return isLock;
    }

    /***********************************************************
     Listeners
     ***********************************************************/
    private class ComponentListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.d(TAG, "onPlayerStateChanged: playWhenReady: " + playWhenReady + " playbackState: " + playbackState);
            switch (playbackState) {
                case Player.STATE_IDLE:
                    playerController.showProgressBar(false);
                    playerController.showRetryBtn(true);
                    break;
                case Player.STATE_BUFFERING:
                    playerController.showProgressBar(true);
                    break;
                case Player.STATE_READY:
                    playerController.showProgressBar(false);
                    playerController.audioFocus();
                    break;
                case Player.STATE_ENDED:
                    playerController.showProgressBar(false);
                    playerController.videoEnded();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            playerController.showProgressBar(false);
            playerController.showRetryBtn(true);
        }
    }

    public static int getScreenWidth(Context context, boolean isIncludeNav) {
        if (isIncludeNav) {
            return context.getResources().getDisplayMetrics().widthPixels + getNavigationBarHeight(context);
        } else {
            return context.getResources().getDisplayMetrics().widthPixels;
        }
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenHeight(Context context, boolean isIncludeNav) {
        if (isIncludeNav) {
            return context.getResources().getDisplayMetrics().heightPixels + getNavigationBarHeight(context);
        } else {
            return context.getResources().getDisplayMetrics().heightPixels;
        }
    }

    public static WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static int getNavigationBarHeight(Context context) {
        if (!hasNavigationBar(context)) {
            return 0;
        }
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android");
        //获取NavigationBar的高度
        return resources.getDimensionPixelSize(resourceId);
    }

    public static Activity scanForActivity(Context context) {
        return context == null ? null : (context instanceof Activity ? (Activity) context : (context instanceof ContextWrapper ? scanForActivity(((ContextWrapper) context).getBaseContext()) : null));
    }

    public static boolean hasNavigationBar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = getWindowManager(context).getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            return realSize.x != size.x || realSize.y != size.y;
        } else {
            boolean menu = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            return !(menu || back);
        }
    }
}
