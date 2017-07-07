package com.meiji.toutiao.binder.media;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.meiji.toutiao.ErrorAction;
import com.meiji.toutiao.R;
import com.meiji.toutiao.bean.media.MediaProfileBean;
import com.meiji.toutiao.database.dao.MediaChannelDao;
import com.meiji.toutiao.util.ImageLoader;
import com.meiji.toutiao.widget.CircleImageView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.drakeet.multitype.ItemViewBinder;

/**
 * Created by Meiji on 2017/7/3.
 */

public class MediaArticleHeaderViewBinder extends ItemViewBinder<MediaProfileBean.DataBean, MediaArticleHeaderViewBinder.ViewHolder> {

    private MediaChannelDao dao = new MediaChannelDao();

    @NonNull
    @Override
    protected MediaArticleHeaderViewBinder.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_media_article_header, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull final ViewHolder holder, @NonNull final MediaProfileBean.DataBean item) {
        try {

            final Context context = holder.itemView.getContext();

            // 设置头条号信息
            String imgUrl = item.getBg_img_url();
            if (!TextUtils.isEmpty(imgUrl)) {
                ImageLoader.loadCenterCrop(context, imgUrl, holder.iv_bg, R.color.viewBackground);
            }
            String avatarUrl = item.getBig_avatar_url();
            if (!TextUtils.isEmpty(imgUrl)) {
                ImageLoader.loadCenterCrop(context, avatarUrl, holder.cv_avatar, R.color.viewBackground);
            }
            holder.tv_name.setText(item.getName());
            holder.tv_desc.setText(item.getDescription());
            holder.tv_sub_count.setText(item.getFollowers_count() + " 订阅量");

            final String mediaId = item.getMedia_id();
            holder.setIsSub(mediaId);

            RxView.clicks(holder.tv_is_sub)
                    .throttleFirst(1, TimeUnit.SECONDS)
                    .observeOn(Schedulers.io())
                    .map(new Function<Object, Boolean>() {
                        @Override
                        public Boolean apply(@NonNull Object o) throws Exception {
                            return dao.queryIsExist(mediaId);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(new Consumer<Boolean>() {
                        @Override
                        public void accept(@NonNull Boolean isExist) throws Exception {
                            if (isExist) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage("取消订阅\" " + item.getName() + " \"?");
                                builder.setPositiveButton(R.string.button_enter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dao.delete(item.getMedia_id());
                                            }
                                        }).start();
                                        holder.tv_is_sub.setText("订阅");
                                        dialog.dismiss();
                                    }
                                });
                                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        holder.tv_is_sub.setText("已订阅");
                                        dialog.dismiss();
                                    }
                                });
                                builder.show();
                            }
                            if (!isExist) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 保存到数据库
                                        dao.add(item.getMedia_id(),
                                                item.getName(),
                                                item.getAvatar_url(),
                                                "news",
                                                item.getFollowers_count(),
                                                item.getDescription(),
                                                "http://toutiao.com/m" + item.getMedia_id());
                                    }
                                }).start();
                                holder.tv_is_sub.setText("已订阅");
                                Toast.makeText(context, "订阅成功", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(@NonNull Boolean isExist) throws Exception {
                            holder.setIsSub(mediaId);
                        }
                    }, ErrorAction.error());

        } catch (Exception e) {
            ErrorAction.print(e);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView iv_bg;
        private CircleImageView cv_avatar;
        private TextView tv_name;
        private TextView tv_desc;
        private TextView tv_is_sub;
        private TextView tv_sub_count;

        public ViewHolder(View itemView) {
            super(itemView);
            this.iv_bg = (ImageView) itemView.findViewById(R.id.iv_bg);
            this.cv_avatar = (CircleImageView) itemView.findViewById(R.id.cv_avatar);
            this.tv_name = (TextView) itemView.findViewById(R.id.tv_name);
            this.tv_desc = (TextView) itemView.findViewById(R.id.tv_desc);
            this.tv_is_sub = (TextView) itemView.findViewById(R.id.tv_is_sub);
            this.tv_sub_count = (TextView) itemView.findViewById(R.id.tv_sub_count);
        }

        private void setIsSub(final String mediaId) {
            Observable
                    .create(new ObservableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                            boolean isExist = dao.queryIsExist(mediaId);
                            e.onNext(isExist);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(@NonNull Boolean isExist) throws Exception {
                            if (isExist) {
                                tv_is_sub.setText("已订阅");
                            } else {
                                tv_is_sub.setText("订阅");
                            }
                        }
                    }, ErrorAction.error());
        }
    }
}
