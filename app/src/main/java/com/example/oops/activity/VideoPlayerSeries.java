package com.example.oops.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.oops.DataClass.EpisodeData;
import com.example.oops.DataClass.SeasonData;
import com.example.oops.R;
import com.example.oops.ResponseClass.CommonResponse;
import com.example.oops.ResponseClass.EpisodeResponse;
import com.example.oops.ResponseClass.SeasonResponse;
import com.example.oops.Utils.AppCommon;
import com.example.oops.Utils.DemoDownloadService;
import com.example.oops.Utils.DownloadTracker;
import com.example.oops.Utils.RecyclerItemClickListener;
import com.example.oops.Utils.ViewUtils;
import com.example.oops.adapter.EpisodeAdapter;
import com.example.oops.adapter.SeasonAdapter;
import com.example.oops.data.database.AppDatabase;
import com.example.oops.data.database.Subtitle;
import com.example.oops.data.database.Video;
import com.example.oops.data.model.VideoSource;
import com.example.oops.retrofit.AppService;
import com.example.oops.retrofit.ServiceGenerator;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoPlayerSeries extends Activity {

    @BindView(R.id.rl_video_player_series)
    RelativeLayout rl_video_player_series;

    @BindView(R.id.txtVideoHeading)
    AppCompatTextView txtVideoHeading;

    @BindView(R.id.txtSoryLine)
    AppCompatTextView txtSoryLine;

    @BindView(R.id.recylerview)
    RecyclerView recylerview;

    @BindView(R.id.seasonRecycleView)
    RecyclerView seasonRecycleView;

    @BindView(R.id.sdvImage)
    SimpleDraweeView sdvImage;

    @BindView(R.id.seasonList)
    RelativeLayout seasonList;

    @BindView(R.id.seasonbtn)
    TextView seasonbtn;

    @BindView(R.id.like)
    ImageView like;

    @BindView(R.id.imgPlayVideo)
    ImageView imgPlayVideo;

    private List<Video> videoUriList;


    /*@BindView(R.id.seasonSpinner)
    Spinner seasonSpinner;*/
    private List<Subtitle> subtitleList = new ArrayList<>();

    ArrayList<SeasonData> data;
    ArrayAdapter<SeasonData> adapter;
    ArrayList<EpisodeData> episodeDataArrayList;

    EpisodeAdapter episodeAdapter;
    SeasonAdapter seasonAdapter;

    private AppDatabase database;
    @BindView(R.id.imgBackPressed)
    AppCompatImageView imgBackPressed;
    ImageView imgDownload;
    List<String> optionsToDownload = new ArrayList<String>();
    DefaultTrackSelector.Parameters qualityParams;
    ProgressDialog pDialog;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;
    private Runnable runnableCode;
    private DownloadTracker downloadTracker;
    private DownloadManager downloadManager;
    private DownloadHelper myDownloadHelper;
    String movieId, json, movieId1;
    String millisInString, name, storyDescription;
    private Handler handler;
    ArrayList<ArrayList<EpisodeData>> list;
    String thumbnailImage, categoryName;
    String see;
    String trailerLink;
    int selectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player_series);
        ButterKnife.bind(this);
        data = new ArrayList<>();

        if (getIntent() != null) {
            movieId = getIntent().getStringExtra("seriesId");
            name = getIntent().getStringExtra("name");
            txtVideoHeading.setText(name);
            movieId1 = String.valueOf(movieId);
            callGetSessionApi(movieId);
        }
        imgBackPressed.setVisibility(View.VISIBLE);
        setLayout();
        initializeDb();


        try {
            DownloadService.start(this, DemoDownloadService.class);
        } catch (IllegalStateException e) {
            DownloadService.startForeground(this, DemoDownloadService.class);
        }
        handler = new Handler();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        millisInString = dateFormat.format(new Date());


        imgDownload = (ImageView) findViewById(R.id.imgDownload);

        runnableCode = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableCode);
    }

    @OnClick(R.id.imgBackPressed)
    public void setImgBackPressed() {
        onBackPressed();
    }

    private void setLayout() {
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recylerview.setLayoutManager(mLayoutManager);
        recylerview.setItemAnimator(new DefaultItemAnimator());


        data = new ArrayList<>();
        seasonAdapter = new SeasonAdapter(this, data);
        RecyclerView.LayoutManager mLayoutManager1 = new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false);
        seasonRecycleView.setLayoutManager(mLayoutManager1);
        seasonRecycleView.setItemAnimator(new DefaultItemAnimator());
        seasonRecycleView.setAdapter(seasonAdapter);
        imgPlayVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoUriList != null) {
                    goToPlayerActivity(makeVideoSource(videoUriList, 0));
                }
            }
        });

    }

    private void initializeDb() {
        database = AppDatabase.Companion.getDatabase(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        database.clearAllTables();
        database = null;

    }

    private void callGetEpisodeList(int position) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            Dialog dialog = ViewUtils.getProgressBar(VideoPlayerSeries.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class, AppCommon.getInstance(this).getToken());
            Map<String, String> entityMap = new HashMap<>();
            entityMap.put("id", String.valueOf(AppCommon.getInstance(this).getId()));
            entityMap.put("userId", String.valueOf(AppCommon.getInstance(this).getUserId()));
            entityMap.put("seasonId", String.valueOf(data.get(position).getSeasonId()));
            Call call = apiService.getEdpisodes(entityMap);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    dialog.dismiss();
                    EpisodeResponse authResponse = (EpisodeResponse) response.body();
                    if (authResponse != null) {
                        Log.i("videoplayer Test", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            if (authResponse.getData() != null) {
                                setDataEpisode(authResponse.getData());
                            }
                        } else {
                            showSnackbar(rl_video_player_series,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    showSnackbar(rl_video_player_series,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });


        } else {
            showSnackbar(rl_video_player_series,getResources().getString(R.string.NoInternet),Snackbar.LENGTH_SHORT);
        }
    }

    private void setDataEpisode(ArrayList<EpisodeData> data) {
        episodeDataArrayList = new ArrayList<>();
        episodeDataArrayList = data;
        videoUriList = new ArrayList<>();
        for (int i = 0; i < episodeDataArrayList.size(); i++) {
            String urlLink = episodeDataArrayList.get(i).getVideoLink();
            videoUriList.add(new Video(urlLink, Long.getLong("zero", 1)));
        }
        if (database.videoDao().getAllUrls().size() == 0) {
            database.videoDao().insertAllVideoUrl(videoUriList);
            //  database.videoDao().insertAllSubtitleUrl(subtitleList);
        }

        episodeAdapter = new EpisodeAdapter(VideoPlayerSeries.this, episodeDataArrayList, new EpisodeAdapter.onItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position, EpisodeData episodeData) {
                selectedPosition = position;
                txtVideoHeading.setText(episodeData.getEpisodeName());
            }
        });

        //episodeAdapter.update(episodeDataArrayList);
        recylerview.setAdapter(episodeAdapter);

        //json = new Gson().toJson(episodeDataArrayList);
    }


    private void makeListOfUri() {


        recylerview.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        selectedPosition = position;
                        //Toast.makeText(VideoPlayerSeries.this, "" + selectedPosition, Toast.LENGTH_LONG).show();
                        txtVideoHeading.setText(episodeDataArrayList.get(position).getEpisodeName());
                    }
                }));

        videoUriList.add(new Video(trailerLink, Long.getLong("zero", 1)));


        if (database.videoDao().getAllUrls().size() == 0) {
            database.videoDao().insertAllVideoUrl(videoUriList);
            database.videoDao().insertAllSubtitleUrl(subtitleList);
        }

    }

    private void getInit() {


    }

    private void callGetSessionApi(String movieId) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            Dialog dialog = ViewUtils.getProgressBar(VideoPlayerSeries.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class, AppCommon.getInstance(this).getToken());
            Map<String, String> entityMap = new HashMap<>();
            entityMap.put("id", String.valueOf(AppCommon.getInstance(this).getId()));
            entityMap.put("userId", String.valueOf(AppCommon.getInstance(this).getUserId()));
            entityMap.put("seriesId", String.valueOf(movieId));
            Call call = apiService.getSeasons(entityMap);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    dialog.dismiss();
                    SeasonResponse authResponse = (SeasonResponse) response.body();
                    if (authResponse != null) {
                        Log.i("Test", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            if (authResponse.getData() != null) {
                                setData(authResponse.getData());
                            }
                               /* setData(authResponse.getData());
                            videoUrl= authResponse.getData().getVideoLink();*/


                        } else {
                            showSnackbar(rl_video_player_series,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    showSnackbar(rl_video_player_series,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);

                }
            });


        } else {
            showSnackbar(rl_video_player_series,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
        }
    }

    private void setData(ArrayList<SeasonData> data) {
        this.data = data;
        getInit();
        seasonAdapter.update(data);
        sdvImage.setImageURI(data.get(0).getThumbnailLink());
        txtSoryLine.setText(data.get(0).getSeasonDetails());
        for (int i = 0; i < data.size(); i++) {

            thumbnailImage = data.get(i).getThumbnailLink();
            storyDescription = data.get(i).getSeasonDetails();


        }

        seasonbtn.setText("Season " + data.get(0).getSeasonNo());
        callGetEpisodeList(0);        //   adapter.notifyDataSetChanged();
    }

    public void setSeasonClick(int adapterPosition) {
        callGetEpisodeList(adapterPosition);
        seasonList.setVisibility(View.GONE);
        seasonbtn.setText("Season " + data.get(adapterPosition).getSeasonNo());
    }

    @OnClick(R.id.seasonbtn)
    void setClick() {
        seasonList.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.like)
    void setLike() {
        if (like.isSelected()) {
            like.setSelected(false);

        } else
            like.setSelected(true);
        addAndRemoveLike(like.isSelected());
    }

    private void addAndRemoveLike(boolean selected) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            Dialog dialog = ViewUtils.getProgressBar(VideoPlayerSeries.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class, AppCommon.getInstance(this).getToken());
            Map<String, String> entityMap = new HashMap<>();
            entityMap.put("id", String.valueOf(AppCommon.getInstance(this).getId()));
            entityMap.put("userId", String.valueOf(AppCommon.getInstance(this).getUserId()));
            entityMap.put("type", String.valueOf("Series"));
            entityMap.put("serMovId", String.valueOf(movieId));

            Call call = apiService.addAndRemoveFavurite(entityMap);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    dialog.dismiss();
                    CommonResponse authResponse = (CommonResponse) response.body();
                    if (authResponse != null) {
                        Log.i("Test", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            if (authResponse.getData() != null) {
                                if (authResponse.getMessage().equals("Added To Favourite Successfully")) {
                                    like.setSelected(true);
                                } else {
                                    like.setSelected(false);
                                }
                            }
                               /* setData(authResponse.getData());
                            videoUrl= authResponse.getData().getVideoLink();*/


                        } else {
                            showSnackbar(rl_video_player_series,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    showSnackbar(rl_video_player_series,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });


        } else {
            showSnackbar(rl_video_player_series,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
        }
    }


    private VideoSource makeVideoSource(List<Video> videos, int index) {
        setVideosWatchLength();
        List<VideoSource.SingleVideo> singleVideos = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {

            singleVideos.add(i, new VideoSource.SingleVideo(
                    videos.get(i).getVideoUrl(),
                    database.videoDao().getAllSubtitles(i + 1),
                    videos.get(i).getWatchedLength())
            );

        }
        return new VideoSource(singleVideos, index);
    }

    private List<Video> setVideosWatchLength() {
        List<Video> videosInDb = database.videoDao().getVideos();
        for (int i = 0; i < videosInDb.size(); i++) {
            //videoUriList.get(i).setWatchedLength(videosInDb.get(i).getWatchedLength());
            videoUriList.get(i).setWatchedLength(videosInDb.get(i).getWatchedLength());
        }
        return videoUriList;
    }


   /* //start player for result due to future features
    public void goToPlayerActivity(VideoSource videoSource) {
        int REQUEST_CODE = 1000;
        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.putExtra("videoSource", videoSource);

        startActivityForResult(intent, REQUEST_CODE);
    }*/

    public void goToPlayerActivity(VideoSource videoSource) {

        Intent intent = new Intent(VideoPlayerSeries.this, PlayerActivity.class);
        intent.putExtra("videoSource", videoSource);
        intent.putExtra("selectedPosition", selectedPosition);
        startActivity(intent);
    }

    public void showSnackbar(View view, String message, int duration)
    {
        Snackbar snackbar= Snackbar.make(view, message, duration);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setBackgroundTint(getResources().getColor(R.color.colorPrimaryDark));
        snackbar.show();
    }

}