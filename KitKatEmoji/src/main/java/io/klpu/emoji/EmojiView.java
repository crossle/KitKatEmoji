package io.klpu.emoji;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmojiView extends LinearLayout {
    private final static String EMOJI_PREFERENCE = "emoji_preferencee";
    private final static String PREF_KEY_LAST_TAB = "last_tab";
    private final static String PREF_KEY_RECENT_EMOJI = "recent_remoji";

    private final static int[] mIcons = {
            R.drawable.ic_emoji_recent_light,
            R.drawable.ic_emoji_people_light,
            R.drawable.ic_emoji_objects_light,
            R.drawable.ic_emoji_nature_light,
            R.drawable.ic_emoji_places_light,
            R.drawable.ic_emoji_symbols_light
    };

    private final static int[] mEmojis = {
            R.array.emoji_recent,
            R.array.emoji_faces,
            R.array.emoji_objects,
            R.array.emoji_nature,
            R.array.emoji_places,
            R.array.emoji_symbols,
    };

    private EventListener mListener;
    private ViewPager mPager;
    private ImageButton mBackSpace;
    private PagerSlidingTabStrip mTabs;
    private EmojiPagerAdapter mPagerAdapter;
    private FrameLayout mRecentsWrap;
    private View mEmptyView;
    private ArrayList<GridView> mGridViews;
    private SharedPreferences mPreference;
    private Handler mHandler = new Handler();
    private boolean mContinueDel;

    public EmojiView(Context context) {
        super(context);
        init();
    }

    public EmojiView(Context context, AttributeSet paramAttributeSet) {
        super(context, paramAttributeSet);
        init();
    }

    public EmojiView(Context context, AttributeSet paramAttributeSet, int paramInt) {
        super(context, paramAttributeSet, paramInt);
        init();
    }

    private void init() {
        mPreference = getContext().getSharedPreferences(EMOJI_PREFERENCE, Context.MODE_PRIVATE);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View root = inflater.inflate(R.layout.emoji_layout, this);
        mTabs = (PagerSlidingTabStrip) root.findViewById(R.id.tabs);
        mPager = (ViewPager) root.findViewById(R.id.pager);
        mBackSpace = (ImageButton) root.findViewById(R.id.back_space);
        mGridViews = new ArrayList<GridView>();

        for (int i = 0; i < mIcons.length; i++) {
            String[] emoji = getResources().getStringArray(mEmojis[i]);
            EmojiGridAdapter emojiGridAdapter = new EmojiGridAdapter(getContext(), emoji);
            GridView gridView = (GridView) inflater.inflate(R.layout.emoji_gridview, null);
            gridView.setAdapter(emojiGridAdapter);
            mGridViews.add(gridView);
            if (i == 0) {
                gridView.setOnItemClickListener(mRecentItemClickListener);
                mRecentsWrap = (FrameLayout) inflater.inflate(R.layout.emoji_recent, null);
                mEmptyView = mRecentsWrap.findViewById(R.id.no_recent);
                mRecentsWrap.addView(gridView);
            } else {
                gridView.setOnItemClickListener(mItemClickListener);
            }
        }

        loadRecent();
        mPagerAdapter = new EmojiPagerAdapter();
        mPager.setAdapter(mPagerAdapter);
        mTabs.setOnPageChangeListener(mOnPageChangeListener);
        mTabs.setViewPager(mPager);
        mPager.setCurrentItem(mPreference.getInt(PREF_KEY_LAST_TAB, 0));
        mBackSpace.setOnClickListener(mBackSpaceClickListener);
        mBackSpace.setOnLongClickListener(mBackSpaceLongClickListener);
        mBackSpace.setOnTouchListener(mBackSpaceTouchListener);
    }

    private OnItemClickListener mItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String selected = (String) view.getTag(view.getId());
            if (mListener != null)
                mListener.onEmojiSelected(selected);
            addToRecent(selected);
        }
    };

    private OnItemClickListener mRecentItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String selected = (String) view.getTag(view.getId());
            if (mListener != null)
                mListener.onEmojiSelected(selected);
        }
    };

    private void addToRecent(String selected) {
        String recentEmoji = mPreference.getString(PREF_KEY_RECENT_EMOJI, null);
        if (TextUtils.isEmpty(recentEmoji)) {
            recentEmoji = selected + ",";
        } else {
            String[] recs = recentEmoji.split(",");
            List<String> list = Arrays.asList(recs);
            List<String> newList = new ArrayList<String>(list);
            for (int i = newList.size() - 1; i >= 0; i--) {
                if (newList.get(i).equals(selected)) {
                    newList.remove(i);
                    break;
                }
            }
            newList.add(selected);
            StringBuilder builder = new StringBuilder();
            for (String str : newList) {
                builder.append(str).append(",");
            }
            recentEmoji = builder.toString();
        }
        mPreference.edit().putString(PREF_KEY_RECENT_EMOJI, recentEmoji).commit();
        loadRecent();
    }

    private void loadRecent() {
        String recentEmoji = mPreference.getString(PREF_KEY_RECENT_EMOJI, null);
        if (!TextUtils.isEmpty(recentEmoji)) {
            mEmptyView.setVisibility(View.GONE);
            String[] recentEmojis = recentEmoji.split(",");
            Collections.reverse(Arrays.asList(recentEmojis));
            EmojiGridAdapter recentAdapter = (EmojiGridAdapter) mGridViews.get(0).getAdapter();
            recentAdapter.setEmoji(recentEmojis);
            recentAdapter.notifyDataSetChanged();
        }
    }

    private OnClickListener mBackSpaceClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onBackspace();
            }
        }
    };

    private OnLongClickListener mBackSpaceLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mListener == null) {
                return false;
            }
            mContinueDel = true;
            mHandler.post(mContinueDelRunnable);
            return false;
        }
    };

    private OnTouchListener mBackSpaceTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && mContinueDel) {
                mContinueDel = false;
                mHandler.removeCallbacks(mContinueDelRunnable);
            }
            return false;
        }
    };

    private Runnable mContinueDelRunnable = new Runnable() {
        @Override
        public void run() {
            if (mContinueDel) {
                mListener.onBackspace();
                mHandler.postDelayed(this, 50);
            }
        }
    };

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int item) {
            mPreference.edit().putInt(PREF_KEY_LAST_TAB, item).commit();
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    class EmojiGridAdapter extends BaseAdapter {
        private String[] mEmojis;
        private LayoutInflater mInflater;

        public EmojiGridAdapter(Context c, String[] emojis) {
            mInflater = LayoutInflater.from(c);
            mEmojis = emojis;
        }

        public void setEmoji(String[] emojis) {
            mEmojis = emojis;
        }

        public int getCount() {
            return mEmojis.length;
        }

        public Object getItem(int position) {
            return mEmojis[position];
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (convertView == null) {
                rowView = mInflater.inflate(R.layout.emoji_cell, null);
                ViewHolder viewHolder = new ViewHolder((ImageView) rowView);
                rowView.setTag(viewHolder);
            }

            ViewHolder viewHolder = (ViewHolder) rowView.getTag();
            int resId = getResources().getIdentifier("emoji_u" + mEmojis[position], "drawable",
                    getContext().getPackageName());
            viewHolder.imageView.setImageResource(resId);
            viewHolder.imageView.setTag(viewHolder.imageView.getId(), mEmojis[position]);
            return rowView;
        }
    }

    static class ViewHolder {
        public ImageView imageView;

        public ViewHolder(ImageView imageView) {
            this.imageView = imageView;
        }
    }

    private class EmojiPagerAdapter extends PagerAdapter implements
            PagerSlidingTabStrip.IconTabProvider {

        private EmojiPagerAdapter() {
        }

        @Override
        public void destroyItem(ViewGroup paramViewGroup, int paramInt, Object paramObject) {
            View localObject;
            if (paramInt == 0) {
                localObject = mRecentsWrap;
            } else {
                localObject = mGridViews.get(paramInt);
            }
            paramViewGroup.removeView(localObject);
        }

        @Override
        public int getCount() {
            return mGridViews.size();
        }

        @Override
        public int getPageIconResId(int paramInt) {
            return mIcons[paramInt];
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View gridView;
            if (position == 0) {
                gridView = mRecentsWrap;
            } else {
                gridView = mGridViews.get(position);
            }
            container.addView(gridView);
            return gridView;
        }

        @Override
        public boolean isViewFromObject(View paramView, Object paramObject) {
            return paramView == paramObject;
        }
    }

    public void setEventListener(EventListener listener) {
        this.mListener = listener;
    }

    public static interface EventListener {
        public void onBackspace();

        public void onEmojiSelected(String res);
    }
}
