package cn.wearbbs.music.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.jackuxl.api.UserApi;
import cn.wearbbs.music.R;
import cn.wearbbs.music.util.SharedPreferencesUtil;
import cn.jackuxl.util.NetWorkUtil;

/**
 * 菜单
 */
public class MenuActivity extends SlideBackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        JSONObject profile = SharedPreferencesUtil.getJSONObject("profile");
        if (SharedPreferencesUtil.getJSONObject("profile").size() >= 5) {
            initUserItem(profile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case 0:
                    JSONObject profile = JSON.parseObject(data.getStringExtra("profile"));
                    initUserItem(profile);
                    startActivity(new Intent(MenuActivity.this,MenuActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    finish();
                    break;
                case 1:
                    startActivity(new Intent(MenuActivity.this,MenuActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    finish();
                    break;
            }
        }
    }

    public void initUserItem(JSONObject profile) {
        TextView tv_name = findViewById(R.id.tv_name);
        tv_name.setText(profile.getString("nickname"));
        TextView tv_id = findViewById(R.id.tv_id);
        tv_id.setText(String.format("ID：%s",profile.getString("userId")));
        ImageView iv_avatar = findViewById(R.id.iv_avatar);
        RequestOptions options = RequestOptions.circleCropTransform().placeholder(R.drawable.ic_baseline_supervised_user_circle_24).error(R.drawable.ic_baseline_supervised_user_circle_24);
        Glide.with(MenuActivity.this).load(profile.getString("avatarUrl").replace("http://", "https://")).apply(options).into(iv_avatar);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.item_fm:
                startActivity(new Intent(MenuActivity.this, MainActivity.class)
                        .putExtra("fm",true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;
            case R.id.item_setting:
                startActivity(new Intent(MenuActivity.this, SettingsActivity.class));
                break;
            case R.id.item_user:
                if (SharedPreferencesUtil.getJSONObject("profile").size() <= 5) {
                    startActivityForResult(new Intent(MenuActivity.this, LoginActivity.class), 0);
                } else {
                    startActivityForResult(new Intent(MenuActivity.this, UserProfileActivity.class), 1);
                }
                break;
            case R.id.item_search:
                startActivity(new Intent(MenuActivity.this, SearchActivity.class));
                break;
            case R.id.item_musicPan:
                startActivity(new Intent(MenuActivity.this, MusicPanActivity.class));
                break;
            case R.id.item_likeList:
                startActivity(new Intent(MenuActivity.this, MusicListActivity.class));
                break;
            case R.id.item_musicLibrary:
                startActivity(new Intent(MenuActivity.this, MusicLibraryActivity.class));
                break;
            case R.id.item_localmusic:
                startActivity(new Intent(MenuActivity.this, LocalMusicNewActivity.class));
                break;
        }
    }

}