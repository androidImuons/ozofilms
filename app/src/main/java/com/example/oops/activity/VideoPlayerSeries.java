package com.example.oops.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oops.DataClass.EpisodeData;
import com.example.oops.DataClass.MovieDeatilsData;
import com.example.oops.DataClass.SeasonData;
import com.example.oops.DataClass.WebSearchResponse;
import com.example.oops.R;
import com.example.oops.ResponseClass.CommonResponse;
import com.example.oops.ResponseClass.EpisodeResponse;
import com.example.oops.ResponseClass.MovieDeatilsResponse;
import com.example.oops.ResponseClass.MoviesSearchResponse;
import com.example.oops.ResponseClass.SeasonResponse;
import com.example.oops.Utils.AppCommon;
import com.example.oops.Utils.ViewUtils;
import com.example.oops.adapter.EpisodeAdapter;
import com.example.oops.adapter.RelatedAdapter;
import com.example.oops.adapter.SeasonAdapter;
import com.example.oops.data.database.AppDatabase;
import com.example.oops.data.database.Subtitle;
import com.example.oops.data.database.Video;
import com.example.oops.data.model.VideoSource;
import com.example.oops.retrofit.AppService;
import com.example.oops.retrofit.ServiceGenerator;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;

import java.util.ArrayList;
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

    private List<Video> videoUriList = new ArrayList<>();

    /*@BindView(R.id.seasonSpinner)
    Spinner seasonSpinner;*/
    private List<Subtitle> subtitleList = new ArrayList<>();

    ArrayList<SeasonData> data;
    ArrayAdapter<SeasonData> adapter;
    ArrayList<EpisodeData> episodeDataArrayList;
    EpisodeAdapter episodeAdapter;
    SeasonAdapter seasonAdapter;
    private AppDatabase database;
    String imageLink;

String movieId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_series);
        ButterKnife.bind(this);
        data = new ArrayList<>();

        if (getIntent() != null) {
             movieId = getIntent().getStringExtra("seriesId");
            String name = getIntent().getStringExtra("name");
            txtVideoHeading.setText(name);
            callGetSessionApi(movieId);
        }

        setLayout();
        initializeDb();
        //getInit();
    }

    private void setLayout() {

        episodeDataArrayList = new ArrayList<>();
        episodeAdapter = new EpisodeAdapter(this, episodeDataArrayList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recylerview.setLayoutManager(mLayoutManager);
        recylerview.setItemAnimator(new DefaultItemAnimator());
        recylerview.setAdapter(episodeAdapter);

        data = new ArrayList<>();
        seasonAdapter = new SeasonAdapter(this, data);
        RecyclerView.LayoutManager mLayoutManager1 = new GridLayoutManager(this ,3 , RecyclerView.VERTICAL , false);
        seasonRecycleView.setLayoutManager(mLayoutManager1);
        seasonRecycleView.setItemAnimator(new DefaultItemAnimator());
        seasonRecycleView.setAdapter(seasonAdapter);
        imgPlayVideo.setOnClickListener(view -> goToPlayerActivity(makeVideoSource(videoUriList, 0)));

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
                        Log.i("Test", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            if (authResponse.getData() != null) {
                                setDataEpisode(authResponse.getData());

//                                List<Epis>

                            }
                               /* setData(authResponse.getData());
                            videoUrl= authResponse.getData().getVideoLink();*/


                        } else {

                            Toast.makeText(VideoPlayerSeries.this, authResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    // loaderView.setVisibility(View.GONE);
                    Toast.makeText(VideoPlayerSeries.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            });


        } else {
            // no internet
            Toast.makeText(this, "Please check your internet", Toast.LENGTH_SHORT).show();
        }
    }

    private void setDataEpisode(ArrayList<EpisodeData> data) {

        episodeDataArrayList = data;
        episodeAdapter.update(episodeDataArrayList);
        makeListOfUri(data);
    }


    private void makeListOfUri(ArrayList<EpisodeData> data) {
        for(int i=0;i<data.size();i++){

             imageLink= data.get(i).getVideoLink();

            Log.d("NSOODODODOD", "setDataEpisode: "+imageLink);
        }
        videoUriList.add(new Video(imageLink , Long.getLong("zero" , 1)));

        /*videoUriList.add(new Video("https://5b44cf20b0388.streamlock.net:8443/vod/smil:bbb.smil/playlist.m3u8", Long.getLong("zero", 1)));

        subtitleList.add(new Subtitle(2, "German", "https://durian.blender.org/wp-content/content/subtitles/sintel_en.srt"));
        subtitleList.add(new Subtitle(2, "French", "https://durian.blender.org/wp-content/content/subtitles/sintel_fr.srt"));
*/
        if (database.videoDao().getAllUrls().size() == 0) {
            database.videoDao().insertAllVideoUrl(videoUriList);
    database.videoDao().insertAllSubtitleUrl(subtitleList);
        }

    }

    private void getInit() {

     /*   adapter =
                new ArrayAdapter<SeasonData>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        seasonSpinner.setAdapter(adapter);
        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                                       int postion, long arg3) {
                // TODO Auto-generated method stub

                callGetEpisodeList(postion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

     */
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

                            Toast.makeText(VideoPlayerSeries.this, authResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    // loaderView.setVisibility(View.GONE);
                    Toast.makeText(VideoPlayerSeries.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            });


        } else {
            // no internet
            Toast.makeText(this, "Please check your internet", Toast.LENGTH_SHORT).show();
        }
    }

    private void setData(ArrayList<SeasonData> data) {
        this.data = data;
        getInit();
        seasonAdapter.update(data);
        sdvImage.setImageURI(data.get(0).getThumbnailLink());
        txtSoryLine.setText(data.get(0).getSeasonDetails());

        seasonbtn.setText("Season "+data.get(0).getSeasonNo());
        callGetEpisodeList(0);        //   adapter.notifyDataSetChanged();
    }

    public void setSeasonClick(int adapterPosition) {
        callGetEpisodeList(adapterPosition);
        seasonList.setVisibility(View.GONE);
        seasonbtn.setText("Season "+data.get(adapterPosition).getSeasonNo());
    }

    @OnClick(R.id.seasonbtn)
    void setClick(){
        seasonList.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.like)
    void  setLike(){
        if(like.isSelected()){
            like.setSelected(false);

        }else
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
                                if(authResponse.getMessage().equals("Added To Favourite Successfully")){
                                    like.setSelected(true);
                                }else {
                                    like.setSelected(false);
                                }
                            }
                               /* setData(authResponse.getData());
                            videoUrl= authResponse.getData().getVideoLink();*/


                        } else {

                            Toast.makeText(VideoPlayerSeries.this, authResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        AppCommon.getInstance(VideoPlayerSeries.this).showDialog(VideoPlayerSeries.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(VideoPlayerSeries.this).clearNonTouchableFlags(VideoPlayerSeries.this);
                    // loaderView.setVisibility(View.GONE);
                    Toast.makeText(VideoPlayerSeries.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            });


        } else {
            // no internet
            Toast.makeText(this, "Please check your internet", Toast.LENGTH_SHORT).show();
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


    //start player for result due to future features
    public void goToPlayerActivity(VideoSource videoSource) {
        int REQUEST_CODE = 1000;
        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.putExtra("videoSource", videoSource);
        startActivityForResult(intent, REQUEST_CODE);
    }

}