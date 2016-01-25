package com.example.mojilib;

import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mojilib.model.MojiModel;
import com.squareup.picasso252.Picasso;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * contains a viewpager that displays emojis. Populating the page is done by the populator
 * Created by Scott Baar on 1/19/2016.
 */
public class ViewPagerPage extends MakeMojiPage implements PagerPopulator.PopulatorObserver{

    PagerPopulator mPopulator;
    ViewPager vp;
    TextView heading;
    int count;
    VPAdapter vpAdapter;
    int mojisPerPage = 10;
    CirclePageIndicator circlePageIndicator;
    private static int MOJI_ITEM_HEIGHT = 50;
    private static final int ROWS = 4;

    public ViewPagerPage (String title,MojiInputLayout mojiInputLayout,PagerPopulator p){
        super(R.layout.mm_vp_page,mojiInputLayout);
        MOJI_ITEM_HEIGHT *=Moji.density;
        mPopulator = p;
        vp = (ViewPager) mView.findViewById(R.id._mm_moji_pager);
        circlePageIndicator = (CirclePageIndicator) mView.findViewById(R.id._mm_vp_indicator);
     //   VPAdapter vpAdapter = new VPAdapter();
      //  vp.setAdapter(vpAdapter);
        heading = (TextView) mView.findViewById(R.id._mm_page_heading);
        heading.setText(title);
        mPopulator.setup(this);

    }
    //called by the populater once a query is complete.
    @Override
    public void onNewDataAvailable(){
        mojisPerPage =Math.max(10,vp.getWidth()/MOJI_ITEM_HEIGHT * ROWS);
        count = mPopulator.getTotalCount();
        vpAdapter = new VPAdapter();
        vp.setAdapter(vpAdapter);
        circlePageIndicator.setViewPager(vp);
        //vpAdapter.notifyDataSetChanged();


    }
    class VPAdapter extends PagerAdapter{

        @Override
        public int getCount() {
            int c = count/mojisPerPage + (count%mojisPerPage>0?1:0);
            return c;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(mMojiInput.getContext()).inflate(R.layout.mm_vp_page_content,container,false);
            container.addView(view);
            RecyclerView rv = (RecyclerView) view;
            MojiGridAdapter gridAdapter = new MojiGridAdapter(new ArrayList<MojiModel>());
            gridAdapter.setMojiModels(mPopulator.populatePage(mojisPerPage,position*mojisPerPage));
            GridLayoutManager glm = new GridLayoutManager(mMojiInput.getContext(),ROWS,GridLayoutManager.HORIZONTAL,false);
            rv.setLayoutManager(glm);
            rv.setAdapter(gridAdapter);
            return view;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }
    public class MojiGridAdapter extends RecyclerView.Adapter<Holder>
    {
        List<MojiModel> mojiModels = new ArrayList<>();

        public MojiGridAdapter (List<MojiModel> models) {
            mojiModels = models;
        }

        public void setMojiModels(List<MojiModel> models){
            mojiModels = models;
            notifyDataSetChanged();

        }
        @Override
        public int getItemCount() {
            return mojiModels.size();
        }


        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.mm_rv_moji_item, parent, false);
            //v.getLayoutParams().height = parent.getHeight()/2;
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            final MojiModel model = mojiModels.get(position);
            holder.imageView.forceDimen(holder.dimen);
            holder.imageView.setModel(model);
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BitmapDrawable bm=null;
                    if (holder.imageView.getDrawable()!=null && holder.imageView.getDrawable() instanceof BitmapDrawable)
                        bm = (BitmapDrawable) holder.imageView.getDrawable();
                    mMojiInput.addMojiModel(model,bm);
                }
            });

        }
    }



    class Holder extends RecyclerView.ViewHolder {
        MojiImageView imageView;
        int dimen;
        public Holder(View v) {
            super(v);
            dimen =mMojiInput.getDefaultSpanDimension();
            imageView = (MojiImageView) v;

        }
    }
}
