package cn.wearbbs.music.fragment;

import static android.app.Activity.RESULT_OK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import api.MusicApi;
import cn.wearbbs.music.R;
import cn.wearbbs.music.ui.ConsoleActivity;
import cn.wearbbs.music.util.SharedPreferencesUtil;
import me.wcy.lrcview.LrcView;

public class LyricsFragment extends Fragment {
    private JSONArray data;
    private static int musicIndex;
    private int currentPosition;
    private LrcView lrcView;
    private boolean local;
    public static LyricsFragment newInstance(Intent intent) {
        LyricsFragment fragment = new LyricsFragment();
        Bundle args = new Bundle();
        args.putString("data", intent.getStringExtra("data"));
        args.putInt("musicIndex", intent.getIntExtra("musicIndex",0));
        args.putBoolean("local",intent.getBooleanExtra("local",false));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyrics, container, false);
        if (getArguments() != null && getArguments().getString("data") != null) {
            data = JSON.parseArray(getArguments().getString("data"));
            musicIndex = getArguments().getInt("musicIndex");
            local = getArguments().getBoolean("local");
            lrcView = view.findViewById(R.id.lv_main);
            updateLyric(requireContext());
            new Thread(() -> {
                while (true) {
                    if (currentPosition != 0) {
                        lrcView.updateTime(currentPosition);
                    }
                }
            }).start();
            view.findViewById(R.id.ll_console).setOnClickListener(v -> {
                startActivityForResult(new Intent(requireContext(), ConsoleActivity.class)
                        .putExtra("data",data.toJSONString())
                        .putExtra("musicIndex",musicIndex)
                        .putExtra("local",local)
                        .putExtra("repeatOne",PlayerFragment.getRepeatOne()),0);
            });
        }
        return view;
    }

    class CurrentPositionBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentPosition = intent.getIntExtra("currentPosition", 0);
        }
    }

    class MusicChangedBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int newIndex = intent.getIntExtra("musicIndex", 0);
            data = JSON.parseArray(intent.getStringExtra("data"));
            if(musicIndex!=newIndex){
                musicIndex=newIndex;
                updateLyric(context);
            }
        }
    }

    CurrentPositionBroadcast currentPositionBroadcast;

    @Override
    public void onAttach(@NotNull Context context) {
        currentPositionBroadcast = new CurrentPositionBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("cn.wearbbs.music.player.position");
        requireActivity().registerReceiver(currentPositionBroadcast, intentFilter);
        super.onAttach(context);
    }

    public void updateLyric(Context context){
        new Thread(() -> {
            if(local){
                String lrcFile = data.getJSONObject(musicIndex).getString("lrcFile");
                StringBuilder lyric = new StringBuilder();

                if(lrcFile!=null){
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(lrcFile));
                        String str;
                        while ((str = in.readLine()) != null) {
                            lyric.append(str);
                            lyric.append("\n");
                        }
                    } catch (IOException ignored) { }

                    if(lyric.length()==0){
                        lrcView.loadLrc("[00:00.00]无歌词");
                    }
                    else{
                        lrcView.loadLrc(lyric.toString());
                    }
                }
                else{
                    lrcView.loadLrc("[00:00.00]无歌词");
                }
            }
            else{
                MusicApi api = new MusicApi(SharedPreferencesUtil.getString("cookie", "", context));
                try {
                    String lyric;
                    if (data.getJSONObject(0).containsKey("simpleSong")) {
                        lyric = api.getMusicLyric(data.getJSONObject(musicIndex).getJSONObject("simpleSong").getString("id"));
                    }
                    else{
                        lyric = api.getMusicLyric(data.getJSONObject(musicIndex).getString("id"));
                    }
                    if(lyric==null){
                        lrcView.loadLrc("[00:00.00]无歌词");
                    }
                    else{
                        lrcView.loadLrc(lyric);
                    }
                } catch (Exception ignored) { }
            }

        }).start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(PlayerFragment.getMusicIndex()!=musicIndex){
            musicIndex = PlayerFragment.getMusicIndex();
            lrcView.loadLrc("[00:00.00]加载中");
            updateLyric(requireContext());
        }
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                PlayerFragment.setRepeatOne(data.getBooleanExtra("repeatOne",false));
            }
        }
    }
}