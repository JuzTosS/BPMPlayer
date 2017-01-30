package com.juztoss.rhythmo.views.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.juztoss.rhythmo.R;
import com.juztoss.rhythmo.models.BaseExplorerElement;
import com.juztoss.rhythmo.utils.SystemHelper;
import com.juztoss.rhythmo.views.items.FlippableButton;

/**
 * Created by JuzTosS on 6/18/2016.
 */
public class BrowserElementHolder extends RecyclerView.ViewHolder
{
    private final TextView mName;
    private final TextView mDesc;
    private final ImageView mIcon;
    private final View mBackgroundLayer;
    private final FlippableButton mAddButton;

    private int mPosition;
    private int mImageRes;
    private int mIconImageRes;
    private IBrowserElementClickListener mListener;
    private BaseExplorerElement mElement;

    public BrowserElementHolder(View view, IBrowserElementClickListener listener)
    {
        super(view);
        mListener = listener;
        view.setOnClickListener(v -> {
            mListener.onItemClick(mPosition);
            BrowserElementHolder.this.onClick();
        });
        mListener = listener;

        mName = ((TextView) itemView.findViewById(R.id.name_text_view));
        mDesc = ((TextView) itemView.findViewById(R.id.desc_text_view));
        mIcon = (ImageView) itemView.findViewById(R.id.element_icon);
        mBackgroundLayer = itemView.findViewById(R.id.rowBackgroundLayer);
        mAddButton = (FlippableButton) itemView.findViewById(R.id.add_icon);
        mAddButton.setOnClickListener(v -> {
            mListener.onActionClick(mPosition);
            BrowserElementHolder.this.onClick();
        });
    }

    private void onClick()
    {
        update(mElement, mPosition, true);
    }

    private void setImageResource(int res, boolean animate)
    {
        if(mImageRes == res) return;

        mImageRes = res;

        if(animate)
            mAddButton.flipTo(res);
        else
            mAddButton.flipInstantlyTo(res);
    }

    public void update(BaseExplorerElement element, int position)
    {
        update(element, position, false);
    }

    private void update(BaseExplorerElement element, int position,  boolean animate)
    {
        mPosition = position;
        mElement = element;
        mName.setText(element.name());
        mDesc.setText(element.description());

        if (element.getIconResource() > 0)
        {
            int iconRes = element.getIconResource();
            if(mIconImageRes != iconRes)
            {
                mIconImageRes = iconRes;
                mIcon.setImageResource(iconRes);
            }

            mIcon.setVisibility(View.VISIBLE);
        }
        else
            mIcon.setVisibility(View.GONE);

        Context context = itemView.getContext();
        if(element.isAddable())
        {
            mAddButton.setVisibility(View.VISIBLE);
            if (element.getAddState() == BaseExplorerElement.AddState.NOT_ADDED)
            {
                mBackgroundLayer.setBackground(null);
                setImageResource(R.drawable.ic_add_circle_black_36dp, animate);
            }
            else if (element.getAddState() == BaseExplorerElement.AddState.ADDED)
            {
                mBackgroundLayer.setBackgroundColor(SystemHelper.getColor(context, R.attr.rAccentSecondaryAlpha));
                setImageResource(R.drawable.ic_remove_circle_black_36dp, animate);
            }
            else if (element.getAddState() == BaseExplorerElement.AddState.PARTLY_ADDED)
            {
                mBackgroundLayer.setBackgroundColor(SystemHelper.getColor(context, R.attr.rAccentSecondaryAlpha));
                setImageResource(R.drawable.ic_remove_circle_outline_black_36dp, animate);
            }
        }else
        {
            mAddButton.setVisibility(View.GONE);
            mBackgroundLayer.setBackground(null);
        }
    }

    public interface IBrowserElementClickListener
    {
        void onItemClick(int position);
        void onActionClick(int position);
    }
}
