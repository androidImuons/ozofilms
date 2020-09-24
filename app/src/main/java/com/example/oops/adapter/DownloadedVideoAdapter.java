package com.example.oops.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.oops.DataClass.CategoryListData;
import com.example.oops.Ooops;
import com.example.oops.R;
import com.example.oops.Utils.AppCommon;
import com.example.oops.Utils.AppUtil;
import com.example.oops.Utils.MyDiffUtilCallback;
import com.example.oops.activity.OfflinePlayerActivity;
import com.example.oops.fragment.DownloadVideo;
import com.example.oops.model.VideoModel;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import butterknife.BindView;

/**
 * Created by Mayur Solanki (mayursolanki120@gmail.com) on 25/02/19, 5:48 PM.
 */
public class DownloadedVideoAdapter extends  RecyclerView.Adapter<DownloadedVideoAdapter.MyViewHolder> { //implements Filterable

        List<Download> videosList;

        List<CategoryListData> categoryListData;
        Context context;
        DownloadVideo downloadActivity;


public class MyViewHolder extends RecyclerView.ViewHolder {
    RelativeLayout rlContainer;
    ImageView imageView;
    TextView tvDownloadVideoTitle;
    TextView tvDownloadVideoPercentage;
    TextView tvDownloadVideoStatus;
    ImageView imgMenuOverFlow;
    ProgressBar progressBarPercentage;



    public MyViewHolder(View view) {
        super(view);
        rlContainer = view.findViewById(R.id.rl_container);
        imageView = view.findViewById(R.id.img_download_banner);
        tvDownloadVideoTitle = view.findViewById(R.id.tv_download_vid_title);
        tvDownloadVideoPercentage = view.findViewById(R.id.tv_downloaded_percentage);
        tvDownloadVideoStatus = view.findViewById(R.id.tv_downloaded_status);
        imgMenuOverFlow = view.findViewById(R.id.img_overflow);
    progressBarPercentage = view.findViewById(R.id.progress_horizontal_percentage);

//            imgDownloadDelete = view.findViewById(R.id.img_delete_download);
//            imgDownloadPlayPause = view.findViewById(R.id.img_download_play_pause);



    }
}


    public DownloadedVideoAdapter(Context context, DownloadVideo downloadActivity) {
        this.context = context;
        this.videosList = new ArrayList<>();
        this.downloadActivity = downloadActivity;
this.categoryListData = new ArrayList<>();


    }

    @Override
    public DownloadedVideoAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_downloaded_video, parent, false);

        return new DownloadedVideoAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DownloadedVideoAdapter.MyViewHolder holder, int position, List<Object> payloads) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("percentDownloaded")) {

                    Download download = (Download) payloads.get(position);
        CategoryListData categoryListData = new CategoryListData();
                    VideoModel videoModel =       AppUtil.getVideoDetail(download.request.id);

//                    if (!videoModel.getVideoName().isEmpty()) {
//                        holder.tvDownloadVideoTitle.setText(categoryListData.getMovieName());
//                    }
//                    holder.imageView.setController(AppCommon.getInstance(this).getDraweeController(sdvImage , data.getBannerLink() , 1024));
//                    Glide.with(this.context)
//                            .load(AppCommon.getInstance(context).getDraweeController())
//                            .diskCacheStrategy(DiskCacheStrategy.ALL)
//                            .into(holder.imageView);
                    DownloadRequest downloadRequest = Ooops.getInstance().getDownloadTracker().getDownloadRequest(download.request.uri);

                    if (download.state == Download.STATE_COMPLETED) {
                        holder.progressBarPercentage.setVisibility(View.GONE);
                    } else {
                        holder.progressBarPercentage.setVisibility(View.VISIBLE);
                        holder.progressBarPercentage.setProgress((int) download.getPercentDownloaded());
                    }
                    String percentage = AppUtil.floatToPercentage(download.getPercentDownloaded());
//                    String downloadInMb = AppUtil.getProgressDisplayLine(download.getBytesDownloaded(), downloadRequest.data.length);


                    if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_COMPLETED) {
                        holder.tvDownloadVideoPercentage.setVisibility(View.VISIBLE);
                        holder.tvDownloadVideoPercentage.setText("Size: " + AppUtil.formatFileSize(download.getBytesDownloaded()) + " | Progress: " + percentage);

                    } else {
                        holder.tvDownloadVideoPercentage.setVisibility(View.INVISIBLE);

                    }
                    holder.tvDownloadVideoStatus.setText(AppUtil.downloadStatusFromId(download));


                }
            }
        }
    }

    @Override
    public void onBindViewHolder(final DownloadedVideoAdapter.MyViewHolder holder, final int position) {

        Download download = videosList.get(position);

        CategoryListData categoryListData= new CategoryListData();

     VideoModel videoModel =   AppUtil.getVideoDetail(download.request.id);

//        if (!categoryListData.getMovieName().isEmpty()) {
           holder.tvDownloadVideoTitle.setText(categoryListData.getMovieName());
//        }

//        holder.imageView.setController(AppCommon.getInstance(context).getDraweeController(holder.imageView ,categoryListData.get(position).getImageLink() , 500));
        if (download.state == Download.STATE_COMPLETED) {
            holder.progressBarPercentage.setVisibility(View.GONE);
        } else {
            holder.progressBarPercentage.setVisibility(View.VISIBLE);
            holder.progressBarPercentage.setProgress((int) download.getPercentDownloaded());
        }
        String percentage = AppUtil.floatToPercentage(download.getPercentDownloaded());
//        String downloadInMb = AppUtil.getProgressDisplayLine(download.getBytesDownloaded(), downloadRequest.data.length);

        if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_COMPLETED) {
            holder.tvDownloadVideoPercentage.setVisibility(View.VISIBLE);
            holder.tvDownloadVideoPercentage.setText("Size: " + AppUtil.formatFileSize(download.getBytesDownloaded()) + " | Progress: " + percentage);

        } else {
            holder.tvDownloadVideoPercentage.setVisibility(View.INVISIBLE);

        }
//        CategoryListData categoryListData = new CategoryListData();


        holder.tvDownloadVideoStatus.setText(AppUtil.downloadStatusFromId(download));


        holder.rlContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(download.state == Download.STATE_COMPLETED){
                    Bundle bundle = new Bundle();
                    bundle.putString("video_url", download.request.id);
                    Intent intent = new Intent(context, OfflinePlayerActivity.class);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                    Log.i("SUNIL2",""+download.request.id);
                }else {
                    downloadActivity.openBottomSheet(download);
                }
            }
        });

        holder.imgMenuOverFlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadActivity.openBottomSheet(download);
            }

        });

    }



    @Override
    public int getItemCount() {
        return videosList.size();
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }


    public Download getItem(int position) {
        return videosList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public void addItems(List<Download> lst) {
        this.videosList = lst;
    }


    public void onNewData(List<Download> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffUtilCallback(newData, videosList));
        diffResult.dispatchUpdatesTo(this);
        this.videosList.clear();
        this.videosList.addAll(newData);
    }



}




