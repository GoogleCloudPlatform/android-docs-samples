/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.conversation.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.google.cloud.android.conversation.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * A {@link android.widget.TextView} that displays text in a speech bubble.
 */
public class BubbleView extends AppCompatTextView {

    public static final int DIRECTION_INCOMING = 1;
    public static final int DIRECTION_OUTGOING = 2;

    private ColorStateList mTintIncoming;
    private ColorStateList mTintOutgoing;

    @IntDef({DIRECTION_INCOMING, DIRECTION_OUTGOING})
    @Retention(RetentionPolicy.SOURCE)
    @interface Direction {
    }

    private int mDirection;

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("WrongConstant")
    public BubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTintIncoming = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.incoming));
        mTintOutgoing = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.outgoing));
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BubbleView);
        setDirection(a.getInt(R.styleable.BubbleView_direction, DIRECTION_INCOMING));
        a.recycle();
    }

    public void setDirection(@Direction int direction) {
        if (mDirection == direction) {
            return;
        }
        mDirection = direction;
        if (mDirection == DIRECTION_INCOMING) {
            setBackgroundResource(R.drawable.bubble_incoming);
            ViewCompat.setBackgroundTintList(this, mTintIncoming);
        } else {
            setBackgroundResource(R.drawable.bubble_outgoing);
            ViewCompat.setBackgroundTintList(this, mTintOutgoing);
        }
    }

    @Direction
    public int getDirection() {
        return mDirection;
    }

}
