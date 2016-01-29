package com.github.mayayeung;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by 华夫饼 on 2015/3/20.
 */
public abstract class SimpleListAdapter<T> extends BaseAdapter {
    protected List<T> mList;

    public SimpleListAdapter(List<T> source) {
        mList = source;
    }

    public SimpleListAdapter() {

    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void changeData(List<T> source) {
        this.mList = source;
    }

    public void appendData(List<T> data) {
        if (data != null && data.size() > 0) {
            if (this.mList == null) {
                this.mList = data;
            } else {
                this.mList.addAll(data);
            }
        }
    }

    public void appendData(int location, List<T> data) {
        if (data != null && data.size() > 0) {
            if (this.mList == null) {
                this.mList = data;
            } else {
                this.mList.addAll(location, data);
            }
        }
    }


    public void removeData(int start, int end) {
        if (this.mList != null) {
            try {
                if (start <= end) {
                    this.mList.subList(start, end).clear();
                }
            } catch (IndexOutOfBoundsException e) {
                return;
            }
        }
    }

    public List<T> getData() {
        return this.mList;
    }

    protected OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(ViewGroup parent, View view, int position, long id);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        T item = mList.get(position);
        final View view = getView(item, position, convertView, parent);
        if (mOnItemClickListener != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(parent, view, position, getItemId(position));
                }
            });
        }
        return view;
    }

    public abstract View getView(T item, int position, View convertView, ViewGroup parent);
}
