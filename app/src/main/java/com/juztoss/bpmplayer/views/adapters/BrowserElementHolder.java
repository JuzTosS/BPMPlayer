package com.juztoss.bpmplayer.views.adapters;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.juztoss.bpmplayer.R;
import com.juztoss.bpmplayer.models.BaseExplorerElement;

/**
 * Created by JuzTosS on 6/18/2016.
 */
public class BrowserElementHolder extends RecyclerView.ViewHolder
{
    private final TextView mName;
    private final TextView mDesc;
    private final ImageView mIcon;
    private final ImageButton mAddButton;

    private int mPosition;
    private IBrowserElementClickListener mListener;

    public BrowserElementHolder(View view, IBrowserElementClickListener listener)
    {
        super(view);
        mListener = listener;
        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mListener.onItemClick(mPosition);
            }
        });
        mListener = listener;

        mName = ((TextView) itemView.findViewById(R.id.name_text_view));
        mDesc = ((TextView) itemView.findViewById(R.id.desc_text_view));
        mIcon = (ImageView) itemView.findViewById(R.id.element_icon);
        mAddButton = (ImageButton) itemView.findViewById(R.id.add_icon);
        mAddButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mListener.onActionClick(mPosition);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public void update(BaseExplorerElement element, int position)
    {
        mPosition = position;
        mName.setText(element.name());
        mDesc.setText(element.description());

        if (element.getIconResource() > 0)
        {
            mIcon.setImageResource(element.getIconResource());
            mIcon.setVisibility(View.VISIBLE);
        }
        else
            mIcon.setVisibility(View.GONE);

        if(element.isAddable())
        {
            mAddButton.setVisibility(View.VISIBLE);
            if (element.getAddState() == BaseExplorerElement.AddState.NOT_ADDED)
                mAddButton.setImageResource(R.drawable.ic_add_circle_black_36dp);
            else if (element.getAddState() == BaseExplorerElement.AddState.ADDED)
                mAddButton.setImageResource(R.drawable.ic_remove_circle_black_36dp);
            else if (element.getAddState() == BaseExplorerElement.AddState.PARTLY_ADDED)
                mAddButton.setImageResource(R.drawable.ic_remove_circle_outline_black_36dp);
        }else
        {
            mAddButton.setVisibility(View.GONE);
        }
    }

    public interface IBrowserElementClickListener
    {
        void onItemClick(int position);
        void onActionClick(int position);
    }
}