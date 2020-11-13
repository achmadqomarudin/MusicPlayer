package com.ldt.musicr.ui.page.subpages;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.ldt.musicr.App;
import com.ldt.musicr.contract.AbsMediaAdapter;
import com.ldt.musicr.helper.menu.MenuHelper;
import com.ldt.musicr.loader.medialoader.LastAddedLoader;

import com.ldt.musicr.loader.medialoader.TopAndRecentlyPlayedTracksLoader;
import com.ldt.musicr.ui.page.MusicServiceNavigationFragment;
import com.ldt.musicr.ui.page.librarypage.song.SongChildAdapter;
import com.ldt.musicr.ui.bottomsheet.OptionBottomSheet;
import com.ldt.musicr.ui.bottomsheet.SortOrderBottomSheet;
import com.ldt.musicr.ui.page.librarypage.artist.ArtistAdapter;
import com.ldt.musicr.ui.widget.fragmentnavigationcontroller.PresentStyle;
import com.ldt.musicr.util.AutoGeneratedPlaylistBitmap;
import com.ldt.musicr.R;
import com.ldt.musicr.loader.medialoader.PlaylistSongLoader;

import com.ldt.musicr.model.Playlist;
import com.ldt.musicr.model.Song;
import com.ldt.musicr.ui.widget.BlurImageViewChildConstraintLayout;
import com.ldt.musicr.util.Tool;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import butterknife.Unbinder;

public class PlaylistPageFragment extends MusicServiceNavigationFragment implements SortOrderBottomSheet.SortOrderChangedListener {
    private static final String TAG ="PlaylistPagerFragment";

    @Override
    public int getPresentTransition() {
        return PresentStyle.ACCORDION_LEFT;
    }

    @BindView(R.id.play_all_button) TextView mPlayAllButton;
    @BindView(R.id.play_all_icon) ImageView mPlayAllIcon;
    @BindView(R.id.shuffle_play_button) TextView mPlayRandomButton;

    @BindView(R.id.playlist_big_rv) RecyclerView mRecyclerView;

    @BindView(R.id.art) ImageView mImage;
    @BindView(R.id.title) TextView mTitle;
    @BindView(R.id.description) TextView mArtist;

    @BindView(R.id.toolbar) Toolbar toolbar;

    @BindView(R.id.background_constraint)
    BlurImageViewChildConstraintLayout back_constraint;
    @BindView(R.id.playlist_pager_collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.play_option_panel) View mPlayOptionPanel;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;

    private final SongChildAdapter mAdapter = new SongChildAdapter();

    Playlist mPlaylist;

    @OnClick(R.id.menu_button)
    void onClickMenu() {
        if(mPlaylist!=null && mPlaylist.id <0)
            OptionBottomSheet.newInstance(MenuHelper.AUTO_PLAYLIST_OPTION,mPlaylist).show(getChildFragmentManager(),"playlist_option_menu");
        else
        OptionBottomSheet.newInstance(MenuHelper.PLAYLIST_OPTION,mPlaylist).show(getChildFragmentManager(),"playlist_option_menu");
    }

    @OnClick(R.id.play_all_panel)
    void playAll() {
    mAdapter.playAll(0,true);
    }

    @OnTouch(R.id.art)
    boolean onTouchArt(View view, MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN) {
           return false;

        }
        return false;
    }

    @OnClick(R.id.shuffle_play_button)
    void playRandom() {

        mAdapter.shuffle();
    }

    public void setTheme() {
        int buttonColor = ArtistAdapter.lighter(Tool.getBaseColor(),0.25f);
        int heavyColor = Tool.getHeavyColor();
        mPlayAllButton.setTextColor(buttonColor);
        mPlayAllIcon.setColorFilter(buttonColor);
        mPlayRandomButton.setTextColor(buttonColor);
        mTitle.setTextColor(Tool.getBaseColor());
        if(mRecyclerView instanceof FastScrollRecyclerView) {
            ((FastScrollRecyclerView)mRecyclerView).setPopupBgColor(heavyColor);
            ((FastScrollRecyclerView)mRecyclerView).setThumbColor(heavyColor);
        }
    }

    @Override
    public void onServiceConnected() {
        refreshData();
    }

    @Override
    public void onServiceDisconnected() {

    }

    @Override
    public void onQueueChanged() {
        refreshData();
    }

    @Override
    public void onPlayingMetaChanged() {
        mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PLAY_STATE_CHANGED);
    }

    @Override
    public void onPaletteChanged() {
        setTheme();
        mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PALETTE_CHANGED);
        super.onPaletteChanged();
    }

    @Override
    public void onPlayStateChanged() {
        mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PLAY_STATE_CHANGED);
    }

    @Override
    public void onRepeatModeChanged() {

    }

    @Override
    public void onShuffleModeChanged() {

    }

    @Override
    public void onMediaStoreChanged() {
        refreshData();
    }

    Bitmap mPreviewBitmap;

    public static PlaylistPageFragment newInstance(Context context, Playlist playlist, @Nullable Bitmap previewBitmap) {
        PlaylistPageFragment fragment = new PlaylistPageFragment();
        fragment.mPlaylist = playlist;
        fragment.mPreviewBitmap = previewBitmap;
        return fragment;
    }


    private int[] getRelativePosition(View v) {
        int[] locationInScreen = new int[2]; // view's position in scrren
        int[] parentLocationInScreen = new int[2]; // parent view's position in screen
        v.getLocationOnScreen(locationInScreen);
        View parentView = (View)v.getParent();
        parentView.getLocationOnScreen(parentLocationInScreen);
        float relativeX = locationInScreen[0] - parentLocationInScreen[0];
        float relativeY = locationInScreen[1] - parentLocationInScreen[1];
        return new int[]{(int) relativeX, (int) relativeY};
    }

    @Override
    public void onDestroyView() {
        if(mLoadPlaylist!=null) mLoadPlaylist.cancel();


        mAdapter.destroy();

        if(mUnbinder!=null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
        super.onDestroyView();
    }

    @Nullable
    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.screen_single_playlist,container,false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter.init(requireContext());
    }

    private Unbinder mUnbinder;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUnbinder = ButterKnife.bind(this,view);
        initSortOrder();
        //mAdapter.MEDIA_LAYOUT_RESOURCE = R.layout.item_song_bigger;
        mAdapter.setSortOrderChangedListener(this);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));

      //  back_constraint.setShadowDeltaRect((int)oneDp*6,(int)oneDp*4,(int)-oneDp*6,(int)oneDp*4);
       // back_constraint.setShadowDeltaRect(0,0,0,0);

        setupToolbar();
        setTheme();

        if(mPreviewBitmap !=null) {
            mImage.setImageBitmap(mPreviewBitmap);
            back_constraint.setBitmapImage(mPreviewBitmap);
            mPreviewBitmap = null;
        }
        setName();
        mSwipeRefresh.setEnabled(false);
        mSwipeRefresh.setColorSchemeResources(R.color.flatOrange);
        mSwipeRefresh.setOnRefreshListener(this::refreshData);
        this.refreshData();
    }

    public static List<Song> getPlaylistWithListId(@NonNull Context context, Playlist list, String sortOrder) {
        Log.d(TAG, "getPlaylistWithListId: "+list.id);
        if(list.name.equals(context.getString(R.string.playlist_last_added))) return LastAddedLoader.getLastAddedSongs(context);
        else if(list.name.equals(context.getString(R.string.playlist_recently_played))) {
           return TopAndRecentlyPlayedTracksLoader.getRecentlyPlayedTracks(context);
        } else if(list.name.equals(context.getString(R.string.playlist_top_tracks))) {
            return TopAndRecentlyPlayedTracksLoader.getTopTracks(context);
        } else {
            List<Song> songlist = new ArrayList<>(PlaylistSongLoader.getPlaylistSongList(context, list.id));
            return songlist;
        }
    }

    private void setName() {
        mTitle.setText(mPlaylist.name);
        List<Song> songs = mAdapter.getData();
        ArrayList<String> names = new ArrayList<>();
        for(int i=0;i<songs.size()&&names.size()<5;i++) {
            Song song = songs.get(i);
            if(!names.contains(song.artistName)) names.add(song.artistName);
        }

       mArtist.setText(TextUtils.join(", ", names));
    }

    private void refreshData() {
        refreshData(true);
    }

    private void refreshData(boolean b) {
        mSwipeRefresh.setRefreshing(b);
        mSwipeRefresh.post(() -> {
            if(mLoadPlaylist!=null) mLoadPlaylist.cancel();
            mLoadPlaylist = new loadPlaylist(PlaylistPageFragment.this);
            mLoadPlaylist.execute();
        });
    }

    @Override
    public void onSetStatusBarMargin(int value) {
        ((ViewGroup.MarginLayoutParams)toolbar.getLayoutParams()).topMargin = value;
        toolbar.requestLayout();
        int padding_top_back_constraint = (int) (56*getResources().getDimension(R.dimen.oneDP) + 2*value);

        ((ViewGroup.MarginLayoutParams)mPlayOptionPanel.getLayoutParams()).topMargin = value;
        back_constraint.setPadding(back_constraint.getPaddingLeft(),padding_top_back_constraint,back_constraint.getPaddingRight(), 0);
    }

    private void setupToolbar() {
        if(getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setDisplayShowHomeEnabled(true);
                ab.setDisplayShowTitleEnabled(false);
            }
        }
    }
    public static void animateAndChangeImageView(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }
    private loadPlaylist mLoadPlaylist;

    private int mCurrentSortOrder = 0;

    private void initSortOrder() {
        if (mPlaylist != null && !mPlaylist.name.isEmpty()) {
            int defaultOrder = 0;
            if(mPlaylist.name.equals(getResources().getString(R.string.playlist_last_added))) defaultOrder = 2;
            mCurrentSortOrder = App.getInstance().getPreferencesUtility().getSharePreferences().getInt("sort_order_playlist_" + mPlaylist.name + "_" + mPlaylist.id, defaultOrder);

        }
    }

    @Override
    public int getSavedOrder() {
        return mCurrentSortOrder;
    }

    @Override
    public void onOrderChanged(int newType, String name) {
        if(mCurrentSortOrder!=newType) {
            mCurrentSortOrder = newType;
            App.getInstance().getPreferencesUtility().getSharePreferences().edit().putInt("sort_order_playlist_"+mPlaylist.name+"_"+mPlaylist.id,mCurrentSortOrder).commit();
            refreshData();
        }
    }

    private static class loadPlaylist extends AsyncTask<Void, Void, List<Song>> {
        PlaylistPageFragment mFragment;
        private loadArtwork mLoadArtwork;

        loadPlaylist(PlaylistPageFragment fragment) {
            mFragment = fragment;
        }

        @Override
        protected void onPostExecute(List<Song> songs) {
            if(mFragment!=null) {
                mFragment.mAdapter.setData(songs);
                mFragment.setName();
                if(mLoadArtwork!=null) mLoadArtwork.cancel();
                mLoadArtwork= new loadArtwork(mFragment);
                mLoadArtwork.execute();
                mFragment.mSwipeRefresh.setRefreshing(false);
                mFragment.mLoadPlaylist = null;
            }
        }

        @Override
        protected List<Song> doInBackground(Void... voids) {
            if(mFragment==null) return null;
            Context context = mFragment.getContext();
            if(context==null) return null;
           return getPlaylistWithListId(mFragment.getContext(),mFragment.mPlaylist, SortOrderBottomSheet.mSortOrderCodes[mFragment.mCurrentSortOrder]);
        }

        public void cancel() {
            if(mLoadArtwork!=null) mLoadArtwork.cancel();
            cancel(true);
            mFragment = null;

        }
    }

    private static class loadArtwork extends AsyncTask<Void,Void,Bitmap> {
        PlaylistPageFragment mFragment;
        loadArtwork(PlaylistPageFragment fragment) {
            mFragment = fragment;
        }
        public void cancel() {
            cancel(true);
            mFragment = null;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if(mFragment==null) return null;
            Bitmap bmp=null;
            try {
                bmp = AutoGeneratedPlaylistBitmap.getBitmap(mFragment.getContext(), mFragment.mAdapter.getData(), false, false);
            } catch (Exception ignore) {}
            return bmp;
            }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(mFragment!=null&&bitmap!=null) {
                mFragment.mImage.setImageBitmap(bitmap);
                //animateAndChangeImageView(mFragment.getContext(),mFragment.mImage,bitmap);
                mFragment.back_constraint.setBitmapImage(bitmap);

                mFragment = null;
            }
        }


    }

}
