// (c)2016 Flipboard Inc, All Rights Reserved.

package com.chenls.testrxjavaretrofit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ZhuangbiListAdapter extends RecyclerView.Adapter {
    List<ZhuangbiImage> images;
    private ProgressDialog progressDialog;
    private boolean isShare;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new DebounceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final DebounceViewHolder debounceViewHolder = (DebounceViewHolder) holder;
        final ZhuangbiImage image = images.get(position);
        Glide.with(holder.itemView.getContext())
                .load(image.image_url)
                .into(debounceViewHolder.imageIv);
        debounceViewHolder.descriptionTv.setText(image.description);
        debounceViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShare = true;
                myClick(holder, image);
            }
        });
        debounceViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                isShare = false;
                myClick(holder, image);
                return true;
            }
        });

    }

    private void myClick(RecyclerView.ViewHolder holder, ZhuangbiImage image) {
        Context context = holder.itemView.getContext();
        ShareTask shareTask = new ShareTask(context);
        shareTask.execute(image.image_url);
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("请稍等...");
        progressDialog.show();
    }

    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    public void setImages(List<ZhuangbiImage> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    static class DebounceViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIv;
        TextView descriptionTv;
        View itemView;

        public DebounceViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageIv = (ImageView) itemView.findViewById(R.id.imageIv);
            descriptionTv = (TextView) itemView.findViewById(R.id.descriptionTv);
        }
    }

    class ShareTask extends AsyncTask<String, Void, File> {
        private final Context context;

        public ShareTask(Context context) {
            this.context = context;
        }

        @Override
        protected File doInBackground(String... params) {
            String url = params[0]; // should be easy to extend to share multiple images at once
            try {
                File fromFile = Glide
                        .with(context)
                        .load(url)
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();// needs to be called on background thread
                File path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                int length = url.length();
                File toFile = new File(path, url.substring(length - 12, length));
                copyFile(fromFile, toFile);
                return toFile;
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            Uri uri = Uri.fromFile(result);
            if (isShare) {
                share(uri);
            } else {
                Toast.makeText(context, "图片已保存，保存路径：" + result.toString(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
        }

        private void share(Uri result) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_SUBJECT, "分享图片");
            intent.putExtra(Intent.EXTRA_TEXT, "Look what I found!");
            intent.putExtra(Intent.EXTRA_STREAM, result);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "分享图片"));
            progressDialog.dismiss();
        }
    }

    public static void copyFile(File fromFile, File toFile) {
        try {
            java.io.FileInputStream fos_from = new java.io.FileInputStream(fromFile);
            java.io.FileOutputStream fos_to = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fos_from.read(bt)) > 0) {
                fos_to.write(bt, 0, c); //将内容写到新文件当中
            }
            fos_from.close();
            fos_to.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
