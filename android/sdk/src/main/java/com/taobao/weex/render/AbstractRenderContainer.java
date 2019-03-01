/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.weex.render;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.taobao.weex.WXSDKInstance;

import java.lang.ref.WeakReference;

public class AbstractRenderContainer extends FrameLayout {

    protected  WeakReference<WXSDKInstance> mSDKInstance;
    protected boolean mHasConsumeEvent = false;

    public AbstractRenderContainer(@NonNull Context context) {
        super(context);
    }

    public AbstractRenderContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractRenderContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AbstractRenderContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSDKInstance(WXSDKInstance instance) {
        mSDKInstance = new WeakReference<>(instance);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        WXSDKInstance instance;
        if (mSDKInstance != null && (instance = mSDKInstance.get()) != null) {
            //re-render instance
            instance.setSize(w, h);
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mHasConsumeEvent = true;
        return super.dispatchTouchEvent(ev);
    }

    public boolean hasConsumeEvent(){
        return mHasConsumeEvent;
    }
}
