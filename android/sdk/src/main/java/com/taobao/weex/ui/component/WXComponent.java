/*
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
package com.taobao.weex.ui.component;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.ComponentObserver;
import com.taobao.weex.IWXActivityStateListener;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.bridge.Invoker;
import com.taobao.weex.common.Constants;
import com.taobao.weex.common.IWXObject;
import com.taobao.weex.common.WXRuntimeException;
import com.taobao.weex.dom.ImmutableDomObject;
import com.taobao.weex.dom.WXAttr;
import com.taobao.weex.dom.WXDomHandler;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.dom.WXDomTask;
import com.taobao.weex.dom.WXEvent;
import com.taobao.weex.dom.WXStyle;
import com.taobao.weex.dom.action.Actions;
import com.taobao.weex.ui.action.WXUIAction;
import com.taobao.weex.ui.action.WXUIPosition;
import com.taobao.weex.ui.action.WXUISize;
import com.taobao.weex.dom.flex.Spacing;
import com.taobao.weex.ui.IFComponentHolder;
import com.taobao.weex.ui.animation.WXAnimationModule;
import com.taobao.weex.ui.component.pesudo.OnActivePseudoListner;
import com.taobao.weex.ui.component.pesudo.PesudoStatus;
import com.taobao.weex.ui.component.pesudo.TouchActivePseudoListener;
import com.taobao.weex.ui.layout.CSSShorthand;
import com.taobao.weex.ui.layout.ContentBoxMeasurement;
import com.taobao.weex.ui.view.border.BorderDrawable;
import com.taobao.weex.ui.view.gesture.WXGesture;
import com.taobao.weex.ui.view.gesture.WXGestureObservable;
import com.taobao.weex.ui.view.gesture.WXGestureType;
import com.taobao.weex.utils.WXDataStructureUtil;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXReflectionUtils;
import com.taobao.weex.utils.WXResourceUtils;
import com.taobao.weex.utils.WXUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * abstract component
 */
public abstract class WXComponent<T extends View> implements IWXObject, IWXActivityStateListener, OnActivePseudoListner {

  public static final String PROP_FIXED_SIZE = "fixedSize";
  public static final String PROP_FS_MATCH_PARENT = "m";
  public static final String PROP_FS_WRAP_CONTENT = "w";

  private int mFixedProp = 0;
  public static int mComponentNum = 0;

  /**
   * package
   **/
  T mHost;

  private volatile WXVContainer mParent;
  private WXSDKInstance mInstance;
  private Context mContext;

  private int mAbsoluteY = 0;
  private int mAbsoluteX = 0;
  private Set<String> mGestureType;

  private BorderDrawable mBackgroundDrawable;
  private int mPreRealWidth = 0;
  private int mPreRealHeight = 0;
  private int mPreRealLeft = 0;
  private int mPreRealTop = 0;
  private int mStickyOffset = 0;
  private WXGesture mGesture;
  private IFComponentHolder mHolder;
  private boolean isUsing = false;
  private List<OnClickListener> mHostClickListeners;
  private List<OnFocusChangeListener> mFocusChangeListeners;
  private Set<String> mAppendEvents = new HashSet<>();
  private WXAnimationModule.AnimationHolder mAnimationHolder;
  private PesudoStatus mPesudoStatus = new PesudoStatus();
  private boolean mIsDestroyed = false;
  private boolean mIsDisabled = false;
  private int mType = TYPE_COMMON;
  private boolean mNeedLayoutOnAnimation = false;

  public static final int TYPE_COMMON = 0;
  public static final int TYPE_VIRTUAL = 1;

  private int mViewPortWidth = 750;

  private String mPageId;
  private String mComponentType;
  private String mParentRef;
  private String mRef;
  private WXUIPosition mLayoutPosition = new WXUIPosition(0, 0, 0, 0);
  private WXUISize mLayoutSize = new WXUISize(0, 0);

  private WXStyle mStyles;
  private WXAttr mAttributes;
  private WXEvent mEvents;
  private CSSShorthand mMargins;
  private CSSShorthand mPaddings;
  private CSSShorthand mBorders;

  private ContentBoxMeasurement contentBoxMeasurement;

  protected void setContentBoxMeasurement(ContentBoxMeasurement contentBoxMeasurement) {
    this.contentBoxMeasurement = contentBoxMeasurement;
    nativeBindMeasurementToWXCore(getInstanceId(), getRef(), this.contentBoxMeasurement);
  }

  public native void nativeBindMeasurementToWXCore(String instanceId, String ref, ContentBoxMeasurement contentBoxMeasurement);

  public @NonNull
  WXStyle getStyles() {
    if (mStyles == null) {
      mStyles = new WXStyle();
    }
    return mStyles;
  }

  public @NonNull
  WXAttr getAttrs() {
    if (mAttributes == null) {
      mAttributes = new WXAttr();
    }
    return mAttributes;
  }

  public @NonNull
  WXEvent getEvents() {
    if (mEvents == null) {
      mEvents = new WXEvent();
    }
    return mEvents;
  }

  /**
   * Get this node's margin, as defined by cssstyle + default margin.
   */
  public @NonNull
  CSSShorthand getMargin() {
    if (mMargins == null) {
      mMargins = new CSSShorthand();
    }
    return mMargins;
  }

  /**
   * Get this node's padding, as defined by cssstyle + default padding.
   */
  public @NonNull
  CSSShorthand getPadding() {
    if (mPaddings == null) {
      mPaddings = new CSSShorthand();
    }
    return mPaddings;
  }

  /**
   * Get this node's border, as defined by cssstyle.
   */
  public @NonNull
  CSSShorthand getBorder() {
    if (mBorders == null) {
      mBorders = new CSSShorthand();
    }
    return mBorders;
  }

  public void addAttr(Map<String, Object> attrs) {
    if (attrs == null || attrs.isEmpty()) {
      return;
    }
    if (mAttributes == null) {
      mAttributes = new WXAttr();
    }
    mAttributes.putAll(attrs);
  }

  public void addStyle(Map<String, Object> styles) {
    if (styles == null || styles.isEmpty()) {
      return;
    }
    if (mStyles == null) {
      mStyles = new WXStyle();
    }
    addStyle(styles, false);
  }

  public void addStyle(Map<String, Object> styles, boolean byPesudo) {
    if (styles == null || styles.isEmpty()) {
      return;
    }
    if (mStyles == null) {
      mStyles = new WXStyle();
    }
    mStyles.putAll(styles, byPesudo);
  }

  public void addEvent(Set<String> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    if (mEvents == null) {
      mEvents = new WXEvent();
    }
    mEvents.addAll(events);
  }

  public void addShorthand(Map<String, String> shorthand) {
    if (!shorthand.isEmpty()) {
      for (Map.Entry<String, String> item : shorthand.entrySet()) {
        String key = item.getKey();
        switch (key) {
          case Constants.Name.MARGIN:
            addMargin(CSSShorthand.EDGE.ALL, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.MARGIN_LEFT:
            addMargin(CSSShorthand.EDGE.LEFT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.MARGIN_TOP:
            addMargin(CSSShorthand.EDGE.TOP, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.MARGIN_RIGHT:
            addMargin(CSSShorthand.EDGE.RIGHT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.MARGIN_BOTTOM:
            addMargin(CSSShorthand.EDGE.BOTTOM, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.BORDER_WIDTH:
            addBorder(CSSShorthand.EDGE.ALL, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.BORDER_TOP_WIDTH:
            addBorder(CSSShorthand.EDGE.TOP, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.BORDER_RIGHT_WIDTH:
            addBorder(CSSShorthand.EDGE.RIGHT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.BORDER_BOTTOM_WIDTH:
            addBorder(CSSShorthand.EDGE.BOTTOM, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.BORDER_LEFT_WIDTH:
            addBorder(CSSShorthand.EDGE.LEFT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.PADDING:
            addPadding(CSSShorthand.EDGE.ALL, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.PADDING_LEFT:
            addPadding(CSSShorthand.EDGE.LEFT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.PADDING_TOP:
            addPadding(CSSShorthand.EDGE.TOP, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.PADDING_RIGHT:
            addPadding(CSSShorthand.EDGE.RIGHT, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
          case Constants.Name.PADDING_BOTTOM:
            addPadding(CSSShorthand.EDGE.BOTTOM, WXUtils.getFloatByViewport(shorthand.get(key), mViewPortWidth));
            break;
        }
      }
    }
  }

  public void addMargin(CSSShorthand.EDGE spacingType, float margin) {
    if (mMargins == null) {
      mMargins = new CSSShorthand();
    }
    mMargins.set(spacingType, margin);
  }

  public void addPadding(CSSShorthand.EDGE spacingType, float padding) {
    if (mPaddings == null) {
      mPaddings = new CSSShorthand();
    }
    mPaddings.set(spacingType, padding);
  }

  public void addBorder(CSSShorthand.EDGE spacingType, float border) {
    if (mBorders == null) {
      mBorders = new CSSShorthand();
    }
    mBorders.set(spacingType, border);
  }

  private void applyStyles(WXComponent component) {
    if (component != null) {
      updateProperties(component.getStyles());
    }
  }

  public void applyStyle(String key, String value) {
    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value))
      setProperty(key, value);
  }

  private void applyAttrs(WXComponent component) {
    if (component != null) {
      updateProperties(component.getAttrs());
    }
  }

  public void applyAttr(String key, String value) {
    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value))
      setProperty(key, value);
  }

  private void applyBorder(WXComponent component) {
    CSSShorthand border = component.getBorder();
    float left = border.get(CSSShorthand.EDGE.LEFT);
    float top = border.get(CSSShorthand.EDGE.TOP);
    float right = border.get(CSSShorthand.EDGE.RIGHT);
    float bottom = border.get(CSSShorthand.EDGE.BOTTOM);

    if (mHost == null) {
      return;
    }

    setBorderWidth(Constants.Name.BORDER_LEFT_WIDTH, left);
    setBorderWidth(Constants.Name.BORDER_TOP_WIDTH, top);
    setBorderWidth(Constants.Name.BORDER_RIGHT_WIDTH, right);
    setBorderWidth(Constants.Name.BORDER_BOTTOM_WIDTH, bottom);
  }

  public void applyPadding(CSSShorthand padding, CSSShorthand border) {
    int left = (int) (padding.get(CSSShorthand.EDGE.LEFT) + border.get(CSSShorthand.EDGE.LEFT));
    int top = (int) (padding.get(CSSShorthand.EDGE.TOP) + border.get(CSSShorthand.EDGE.TOP));
    int right = (int) (padding.get(CSSShorthand.EDGE.RIGHT) + border.get(CSSShorthand.EDGE.RIGHT));
    int bottom = (int) (padding.get(CSSShorthand.EDGE.BOTTOM) + border.get(CSSShorthand.EDGE.BOTTOM));

    if (mHost == null) {
      return;
    }

    mHost.setPadding(left, top, right, bottom);
  }

  private void applyEvents() {
    if (mEvents == null || mEvents.isEmpty())
      return;
    for (String type : mEvents) {
      applyEvent(type);
    }
    setActiveTouchListener();
  }

  /**
   * Do not use this method to add event, this only apply event already add to DomObject.
   *
   * @param type
   */
  public void applyEvent(String type) {

    if (TextUtils.isEmpty(type) || mAppendEvents.contains(type)) {
      return;
    }
    mAppendEvents.add(type);

    View view = getRealView();
    if (type.equals(Constants.Event.CLICK) && view != null) {
      addClickListener(mClickEventListener);
    } else if ((type.equals(Constants.Event.FOCUS) || type.equals(Constants.Event.BLUR))) {
      addFocusChangeListener(new OnFocusChangeListener() {
        public void onFocusChange(boolean hasFocus) {
          Map<String, Object> params = new HashMap<>();
          params.put("timeStamp", System.currentTimeMillis());
          fireEvent(hasFocus ? Constants.Event.FOCUS : Constants.Event.BLUR, params);
        }
      });
    } else if (view != null &&
            needGestureDetector(type)) {
      if (view instanceof WXGestureObservable) {
        if (mGesture == null) {
          mGesture = new WXGesture(this, mContext);
          boolean isPreventMove = WXUtils.getBoolean(getAttrs().get(Constants.Name.PREVENT_MOVE_EVENT), false);
          mGesture.setPreventMoveEvent(isPreventMove);
        }
        mGestureType.add(type);
        ((WXGestureObservable) view).registerGestureListener(mGesture);
      } else {
        WXLogUtils.e(view.getClass().getSimpleName() + " don't implement " +
                "WXGestureObservable, so no gesture is supported.");
      }
    } else {
      Scrollable scroller = getParentScroller();
      if (type.equals(Constants.Event.APPEAR) && scroller != null) {
        scroller.bindAppearEvent(this);
      }
      if (type.equals(Constants.Event.DISAPPEAR) && scroller != null) {
        scroller.bindDisappearEvent(this);
      }
    }
  }

  public String getAttrByKey(String key) {
    return "default";
  }

  //Holding the animation bean when component is uninitialized
  public void postAnimation(WXAnimationModule.AnimationHolder holder) {
    this.mAnimationHolder = holder;
  }

  private OnClickListener mClickEventListener = new OnClickListener() {
    @Override
    public void onHostViewClick() {
      Map<String, Object> param = WXDataStructureUtil.newHashMapWithExpectedSize(1);
      Map<String, Object> position = WXDataStructureUtil.newHashMapWithExpectedSize(4);
      int[] location = new int[2];
      mHost.getLocationOnScreen(location);
      position.put("x", WXViewUtils.getWebPxByWidth(location[0], mInstance.getInstanceViewPortWidth()));
      position.put("y", WXViewUtils.getWebPxByWidth(location[1], mInstance.getInstanceViewPortWidth()));
      position.put("width", WXViewUtils.getWebPxByWidth(getLayoutWidth(), mInstance.getInstanceViewPortWidth()));
      position.put("height", WXViewUtils.getWebPxByWidth(getLayoutHeight(), mInstance.getInstanceViewPortWidth()));
      param.put(Constants.Name.POSITION, position);
      fireEvent(Constants.Event.CLICK, param);
    }
  };

  public String getInstanceId() {
    return mInstance.getInstanceId();
  }

  public Rect getComponentSize() {
    Rect size = new Rect();
    if (mHost != null) {
      int[] location = new int[2];
      int[] anchor = new int[2];
      mHost.getLocationOnScreen(location);
      mInstance.getContainerView().getLocationOnScreen(anchor);

      int left = location[0] - anchor[0];
      int top = (location[1] - mStickyOffset) - anchor[1];
      int width = (int) getLayoutWidth();
      int height = (int) getLayoutHeight();
      size.set(left, top, left + width, top + height);
    }
    return size;
  }

  public final void invoke(String method, JSONArray args) {
    final Invoker invoker = mHolder.getMethodInvoker(method);
    if (invoker != null) {
      try {
        getInstance()
                .getNativeInvokeHelper()
                .invoke(this, invoker, args);

      } catch (Exception e) {
        WXLogUtils.e("[WXComponent] updateProperties :" + "class:" + getClass() + "method:" + invoker.toString() + " function " + WXLogUtils.getStackTrace(e));
      }
    } else {
      onInvokeUnknownMethod(method, args);
    }
  }

  /**
   * Will be invoked when request method not found.
   * Subclass should override this method, If you return hard-code method list in {@link IFComponentHolder#getMethods()}
   *
   * @param method
   * @param args
   */
  protected void onInvokeUnknownMethod(String method, JSONArray args) {

  }

  public String getParentRef() {
    return mParentRef;
  }

  public interface OnClickListener {
    void onHostViewClick();
  }

  interface OnFocusChangeListener {
    void onFocusChange(boolean hasFocus);
  }

  @Deprecated
  public WXComponent(WXSDKInstance instance, WXVContainer parent, String instanceId, boolean isLazy, WXUIAction action) {
    this(instance, parent, isLazy, action);
  }

  @Deprecated
  public WXComponent(WXSDKInstance instance, WXVContainer parent, boolean isLazy, WXUIAction action) {
    this(instance, parent, action);
  }

  public WXComponent(WXSDKInstance instance, WXVContainer parent, WXUIAction action) {
    this(instance, parent, TYPE_COMMON, action);
  }

  public WXComponent(WXSDKInstance instance, WXVContainer parent, int type, WXUIAction action) {
    mPageId = action.mPageId;
    mComponentType = action.mComponentType;
    mParentRef = action.mParentRef;
    mRef = action.mRef;
    mLayoutPosition = action.mLayoutPosition;
    mLayoutSize = action.mLayoutSize;

    mInstance = instance;
    mContext = mInstance.getContext();
    mParent = parent;
    mType = type;
    mGestureType = new HashSet<>();
    ++mComponentNum;
    if (instance != null)
      mViewPortWidth = instance.getInstanceViewPortWidth();

    onCreate();
    ComponentObserver observer;
    if ((observer = getInstance().getComponentObserver()) != null) {
      observer.onCreate(this);
    }
  }

  private void copyData(WXComponent component) {
    mPageId = component.getPageId();
    mComponentType = component.getComponentType();
    mParentRef = component.getParentRef();
    mRef = component.getRef();
    mParent = component.getParent();
    mType = component.getType();
  }

  protected void onCreate() {

  }

  public void bindHolder(IFComponentHolder holder) {
    mHolder = holder;
  }


  public WXSDKInstance getInstance() {
    return mInstance;
  }

  public Context getContext() {
    return mContext;
  }

  /**
   * Find component by component reference.
   *
   * @param ref
   * @return
   */
  protected final WXComponent findComponent(String ref) {
    if (mInstance != null && ref != null) {
      return WXSDKManager.getInstance()
              .getWXRenderManager()
              .getWXComponent(mInstance.getInstanceId(), ref);
    }
    return null;
  }

  public final void fireEvent(String type) {
    fireEvent(type, null);
  }

  public final void fireEvent(String type, Map<String, Object> params) {
    fireEvent(type, params, null);
  }

  protected final void fireEvent(String type, Map<String, Object> params, Map<String, Object> domChanges) {
    if (mInstance != null) {
      mInstance.fireEvent(mRef, type, params, domChanges);
    }
  }

  /**
   * The view is created as needed
   *
   * @return true for lazy
   */
  public boolean isLazy() {
    return mParent != null && mParent.isLazy();
  }

  protected final void addFocusChangeListener(OnFocusChangeListener l) {
    View view;
    if (l != null && (view = getRealView()) != null) {
      if (mFocusChangeListeners == null) {
        mFocusChangeListeners = new ArrayList<>();
        view.setFocusable(true);
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
          @Override
          public void onFocusChange(View v, boolean hasFocus) {
            for (OnFocusChangeListener listener : mFocusChangeListeners) {
              if (listener != null) {
                listener.onFocusChange(hasFocus);
              }
            }
          }
        });
      }
      mFocusChangeListeners.add(l);
    }
  }

  protected final void addClickListener(OnClickListener l) {
    View view;
    if (l != null && (view = getRealView()) != null) {
      if (mHostClickListeners == null) {
        mHostClickListeners = new ArrayList<>();
        view.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (mGesture != null && mGesture.isTouchEventConsumedByAdvancedGesture()) {
              //event is already consumed by gesture
              return;
            }
            for (OnClickListener listener : mHostClickListeners) {
              if (listener != null) {
                listener.onHostViewClick();
              }
            }
          }
        });
      }
      mHostClickListeners.add(l);

    }
  }

  protected final void removeClickListener(OnClickListener l) {
    mHostClickListeners.remove(l);
  }

  public void bindData(WXComponent component) {
    if (!isLazy()) {
      if (component == null) {
        component = this;
      }
      copyData(component);
      applyStyles(component);
      applyAttrs(component);
      applyBorder(component);
      updateExtra(component.getExtra());
    }
  }

  public void applyLayoutAndEvent(WXComponent component) {
    if (!isLazy()) {
      if (component == null) {
        component = this;
      }
      copyData(component);
      setLayout(component);
      applyPadding(component.getPadding(), component.getBorder());
      applyEvents();
    }
  }

  public void updateDemission(WXUIAction action) {
    mLayoutPosition = action.mLayoutPosition;
    mLayoutSize = action.mLayoutSize;
  }

  public void refreshData(WXComponent component) {

  }

  protected BorderDrawable getOrCreateBorder() {
    if (mBackgroundDrawable == null) {
      Drawable backgroundDrawable = mHost.getBackground();
      WXViewUtils.setBackGround(mHost, null);
      mBackgroundDrawable = new BorderDrawable();
      if (backgroundDrawable == null) {
        WXViewUtils.setBackGround(mHost, mBackgroundDrawable);
      } else {
        //TODO Not strictly clip according to background-clip:border-box
        WXViewUtils.setBackGround(mHost, new LayerDrawable(new Drawable[]{
                mBackgroundDrawable, backgroundDrawable}));
      }
    }
    return mBackgroundDrawable;
  }

  /**
   * layout view
   */
  public final void setLayout(WXComponent component) {

    if (TextUtils.isEmpty(component.getPageId()) || TextUtils.isEmpty(component.getComponentType())
            || TextUtils.isEmpty(component.getRef()) || component.getLayoutPosition() == null
            || component.getLayoutSize() == null) {
      return;
    }

    mLayoutPosition = component.getLayoutPosition();
    mLayoutSize = component.getLayoutSize();
    mViewPortWidth = component.getViewPortWidth();
    mPaddings = component.getPadding();
    mMargins = component.getMargin();
    mBorders = component.getBorder();

    boolean nullParent = mParent == null;//parent is nullable

    //offset by sibling
    int siblingOffset = nullParent ? 0 : mParent.getChildrenLayoutTopOffset();

    int realWidth = (int) WXViewUtils.getRealPxByWidth(getLayoutSize().getWidth(), mViewPortWidth);
    int realHeight = (int) WXViewUtils.getRealPxByWidth(getLayoutSize().getHeight(), mViewPortWidth);
    int realLeft = (int) (WXViewUtils.getRealPxByWidth(getLayoutPosition().getLeft(), mViewPortWidth) -
            getPadding().get(CSSShorthand.EDGE.LEFT) - getBorder().get(CSSShorthand.EDGE.LEFT));
    int realTop = (int) (WXViewUtils.getRealPxByWidth(getLayoutPosition().getTop(), mViewPortWidth) -
            getPadding().get(CSSShorthand.EDGE.TOP) - getBorder().get(CSSShorthand.EDGE.TOP)) + siblingOffset;
    int realRight = (int) getMargin().get(CSSShorthand.EDGE.RIGHT);
    int realBottom = (int) getMargin().get(CSSShorthand.EDGE.BOTTOM);

    if (mPreRealWidth == realWidth && mPreRealHeight == realHeight && mPreRealLeft == realLeft && mPreRealTop == realTop) {
      return;
    }

    mAbsoluteY = (int) (nullParent ? 0 : mParent.getAbsoluteY() + getLayoutY());
    mAbsoluteX = (int) (nullParent ? 0 : mParent.getAbsoluteX() + getLayoutX());

    //calculate first screen time
    if (!mInstance.mEnd && !(mHost instanceof ViewGroup) && mAbsoluteY + realHeight > mInstance.getWeexHeight() + 1) {
      mInstance.firstScreenRenderFinished();
    }

    if (mHost == null) {
      return;
    }

    MeasureOutput measureOutput = measure(realWidth, realHeight);
    realWidth = measureOutput.width;
    realHeight = measureOutput.height;

    //fixed style
    if (isFixed()) {
      setFixedHostLayoutParams(mHost, realWidth, realHeight, realLeft, realRight, realTop, realBottom);
    } else {
      setHostLayoutParams(mHost, realWidth, realHeight, realLeft, realRight, realTop, realBottom);
    }

    mPreRealWidth = realWidth;
    mPreRealHeight = realHeight;
    mPreRealLeft = realLeft;
    mPreRealTop = realTop;

    onFinishLayout();
  }


  public int getLayoutTopOffsetForSibling() {
    return 0;
  }

  protected void setHostLayoutParams(T host, int width, int height, int left, int right, int top, int bottom) {
    ViewGroup.LayoutParams lp;
    if (mParent == null) {
      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
      params.setMargins(left, top, right, bottom);
      lp = params;
    } else {
      lp = mParent.getChildLayoutParams(this, host, width, height, left, right, top, bottom);
    }
    if (lp != null) {
      mHost.setLayoutParams(lp);
    }
  }

  private void setFixedHostLayoutParams(T host, int width, int height, int left, int right, int top, int bottom) {
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    params.width = width;
    params.height = height;
    params.setMargins(left, top, right, bottom);
    host.setLayoutParams(params);
    mInstance.moveFixedView(host);

    if (WXEnvironment.isApkDebugable()) {
      WXLogUtils.d("Weex_Fixed_Style", "WXComponent:setLayout :" + left + " " + top + " " + width + " " + height);
      WXLogUtils.d("Weex_Fixed_Style", "WXComponent:setLayout Left:" + getStyles().getLeft() + " " + (int) getStyles().getTop());
    }
  }

  /**
   * After component's layout result is apply to view. May be invoke multiple times since
   * DOM can be changed in js runtime.
   */
  protected void onFinishLayout() {
    Object param = getStyles() != null ? getStyles().get(Constants.Name.BACKGROUND_IMAGE) : null;
    if (param != null) {
      setBackgroundImage(param.toString());
    }
  }

  public float getLayoutWidth() {
    return mLayoutSize == null ? 0 : WXViewUtils.getRealPxByWidth(mLayoutSize.getWidth(), mViewPortWidth);
  }

  public float getLayoutHeight() {
    return mLayoutSize == null ? 0 : WXViewUtils.getRealPxByWidth(mLayoutSize.getHeight(), mViewPortWidth);
  }

  public void updateExtra(Object extra) {

  }

  /**
   * measure
   */
  protected MeasureOutput measure(int width, int height) {
    MeasureOutput measureOutput = new MeasureOutput();

    if (mFixedProp != 0) {
      measureOutput.width = mFixedProp;
      measureOutput.height = mFixedProp;
    } else {
      measureOutput.width = width;
      measureOutput.height = height;
    }
    return measureOutput;
  }


  @Deprecated
  public void updateProperties(Map<String, Object> props) {
    if (props == null || (mHost == null && !isVirtualComponent())) {
      return;
    }

    for (Map.Entry<String, Object> entry : props.entrySet()) {
      String key = entry.getKey();
      Object param = entry.getValue();
      String value = WXUtils.getString(param, null);
      if (TextUtils.isEmpty(value)) {
        param = convertEmptyProperty(key, value);
      }
      if (!setProperty(key, param)) {
        if (mHolder == null) {
          return;
        }
        Invoker invoker = mHolder.getPropertyInvoker(key);
        if (invoker != null) {
          try {
            Type[] paramClazzs = invoker.getParameterTypes();
            if (paramClazzs.length != 1) {
              WXLogUtils.e("[WXComponent] setX method only one parameter：" + invoker);
              return;
            }
            param = WXReflectionUtils.parseArgument(paramClazzs[0], param);
            invoker.invoke(this, param);
          } catch (Exception e) {
            WXLogUtils.e("[WXComponent] updateProperties :" + "class:" + getClass() + "method:" + invoker.toString() + " function " + WXLogUtils.getStackTrace(e));
          }
        }
      }
    }
    readyToRender();
  }

  /**
   * Apply styles and attributes.
   *
   * @param key   name of argument
   * @param param value of argument
   * @return true means that the property is consumed
   */
  protected boolean setProperty(String key, Object param) {
    switch (key) {
      case Constants.Name.PREVENT_MOVE_EVENT:
        if (mGesture != null) {
          mGesture.setPreventMoveEvent(WXUtils.getBoolean(param, false));
        }
        return true;
      case Constants.Name.DISABLED:
        Boolean disabled = WXUtils.getBoolean(param, null);
        if (disabled != null) {
          setDisabled(disabled);
          setPseudoClassStatus(Constants.PSEUDO.DISABLED, disabled);
        }
        return true;
      case Constants.Name.POSITION:
        String position = WXUtils.getString(param, null);
        if (position != null)
          setSticky(position);
        return true;
      case Constants.Name.BACKGROUND_COLOR:
        String bgColor = WXUtils.getString(param, null);
        if (bgColor != null)
          setBackgroundColor(bgColor);
        return true;
      case Constants.Name.BACKGROUND_IMAGE:
        String bgImage = WXUtils.getString(param, null);
        if (bgImage != null && mHost != null) {
          setBackgroundImage(bgImage);
        }
        return true;
      case Constants.Name.OPACITY:
        Float opacity = WXUtils.getFloat(param, null);
        if (opacity != null)
          setOpacity(opacity);
        return true;
      case Constants.Name.BORDER_RADIUS:
      case Constants.Name.BORDER_TOP_LEFT_RADIUS:
      case Constants.Name.BORDER_TOP_RIGHT_RADIUS:
      case Constants.Name.BORDER_BOTTOM_RIGHT_RADIUS:
      case Constants.Name.BORDER_BOTTOM_LEFT_RADIUS:
        Float radius = WXUtils.getFloat(param, null);
        if (radius != null)
          setBorderRadius(key, radius);
        return true;
      case Constants.Name.BORDER_STYLE:
      case Constants.Name.BORDER_RIGHT_STYLE:
      case Constants.Name.BORDER_BOTTOM_STYLE:
      case Constants.Name.BORDER_LEFT_STYLE:
      case Constants.Name.BORDER_TOP_STYLE:
        String border_style = WXUtils.getString(param, null);
        if (border_style != null)
          setBorderStyle(key, border_style);
        return true;
      case Constants.Name.BORDER_COLOR:
      case Constants.Name.BORDER_TOP_COLOR:
      case Constants.Name.BORDER_RIGHT_COLOR:
      case Constants.Name.BORDER_BOTTOM_COLOR:
      case Constants.Name.BORDER_LEFT_COLOR:
        String border_color = WXUtils.getString(param, null);
        if (border_color != null)
          setBorderColor(key, border_color);
        return true;
      case Constants.Name.VISIBILITY:
        String visibility = WXUtils.getString(param, null);
        if (visibility != null)
          setVisibility(visibility);
        return true;
      case Constants.Name.ELEVATION:
        if (param != null) {
          updateElevation();
        }
        return true;
      case PROP_FIXED_SIZE:
        String fixedSize = WXUtils.getString(param, PROP_FS_MATCH_PARENT);
        setFixedSize(fixedSize);
        return true;
      case Constants.Name.ARIA_LABEL:
        String label = WXUtils.getString(param, "");
        setAriaLabel(label);
        return true;
      case Constants.Name.ARIA_HIDDEN:
        boolean isHidden = WXUtils.getBoolean(param, false);
        setAriaHidden(isHidden);
        return true;
      case Constants.Name.WIDTH:
      case Constants.Name.MIN_WIDTH:
      case Constants.Name.MAX_WIDTH:
      case Constants.Name.HEIGHT:
      case Constants.Name.MIN_HEIGHT:
      case Constants.Name.MAX_HEIGHT:
      case Constants.Name.ALIGN_ITEMS:
      case Constants.Name.ALIGN_SELF:
      case Constants.Name.FLEX:
      case Constants.Name.FLEX_DIRECTION:
      case Constants.Name.JUSTIFY_CONTENT:
      case Constants.Name.FLEX_WRAP:
      case Constants.Name.MARGIN:
      case Constants.Name.MARGIN_TOP:
      case Constants.Name.MARGIN_LEFT:
      case Constants.Name.MARGIN_RIGHT:
      case Constants.Name.MARGIN_BOTTOM:
      case Constants.Name.PADDING:
      case Constants.Name.PADDING_TOP:
      case Constants.Name.PADDING_LEFT:
      case Constants.Name.PADDING_RIGHT:
      case Constants.Name.PADDING_BOTTOM:
      case Constants.Name.BORDER_WIDTH:
      case Constants.Name.BORDER_TOP_WIDTH:
      case Constants.Name.BORDER_RIGHT_WIDTH:
      case Constants.Name.BORDER_BOTTOM_WIDTH:
      case Constants.Name.BORDER_LEFT_WIDTH:
      case Constants.Name.LEFT:
      case Constants.Name.TOP:
      case Constants.Name.RIGHT:
      case Constants.Name.BOTTOM:
        return true;
      default:
        return false;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  protected void setAriaHidden(boolean isHidden) {
    View host = getHostView();
    if (host != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      host.setImportantForAccessibility(isHidden ? View.IMPORTANT_FOR_ACCESSIBILITY_NO : View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
  }

  protected void setAriaLabel(String label) {
    View host = getHostView();
    if (host != null) {
      host.setContentDescription(label);
    }
  }

  /**
   * Avoid large size view fail in GPU-Animation.
   *
   * @param fixedSize
   */
  private void setFixedSize(String fixedSize) {
    if (PROP_FS_MATCH_PARENT.equals(fixedSize)) {
      mFixedProp = ViewGroup.LayoutParams.MATCH_PARENT;
    } else if (PROP_FS_WRAP_CONTENT.equals(fixedSize)) {
      mFixedProp = ViewGroup.LayoutParams.WRAP_CONTENT;
    } else {
      mFixedProp = 0;
      return;
    }
    if (mHost != null) {
      ViewGroup.LayoutParams layoutParams = mHost.getLayoutParams();
      if (layoutParams != null) {
        layoutParams.height = mFixedProp;
        layoutParams.width = mFixedProp;
        mHost.setLayoutParams(layoutParams);
      }
    }
  }

  /**
   * Add new event to component,this will post a task to DOM thread to add event.
   *
   * @param type
   */
  protected void appendEventToDOM(String type) {
    WXSDKManager.getInstance().getWXDomManager().postAction(getInstanceId(), Actions.getAddEvent(getRef(), type), false);
  }

  public View getRealView() {
    return mHost;
  }

  /**
   * Judge whether need to set an onTouchListener.<br>
   * As there is only one onTouchListener in each view, so all the gesture that use onTouchListener should put there.
   *
   * @param type eventType {@link com.taobao.weex.common.Constants.Event}
   * @return true for set an onTouchListener, otherwise false
   */
  private boolean needGestureDetector(String type) {
    if (mHost != null) {
      for (WXGestureType gesture : WXGestureType.LowLevelGesture.values()) {
        if (type.equals(gesture.toString())) {
          return true;
        }
      }
      for (WXGestureType gesture : WXGestureType.HighLevelGesture.values()) {
        if (type.equals(gesture.toString())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * get Scroller components
   */
  public Scrollable getParentScroller() {
    WXComponent component = this;
    WXVContainer container;
    Scrollable scroller;
    for (; ; ) {
      container = component.getParent();
      if (container == null) {
        return null;
      }
      if (container instanceof Scrollable) {
        scroller = (Scrollable) container;
        return scroller;
      }
      if (container.getRef().equals(WXDomObject.ROOT)) {
        return null;
      }
      component = container;
    }
  }

  /**
   * get Scroller components
   */
  @Nullable
  public Scrollable getFirstScroller() {
    if (this instanceof Scrollable) {
      return (Scrollable) this;
    }
    return null;
  }

  public WXVContainer getParent() {
    return mParent;
  }

  public String getRef() {
    return mRef;
  }

  /**
   * create view
   */
  public final void createView() {
    if (!isLazy()) {
      createViewImpl();
    }
  }

  protected void createViewImpl() {
    if (mContext != null) {
      mHost = initComponentHostView(mContext);
      if (mHost == null && !isVirtualComponent()) {
        //compatible
        initView();
      }
      if (mHost != null) {
        mHost.setId(WXViewUtils.generateViewId());
        ComponentObserver observer;
        if ((observer = getInstance().getComponentObserver()) != null) {
          observer.onViewCreated(this, mHost);
        }
      }
      onHostViewInitialized(mHost);
    } else {
      WXLogUtils.e("createViewImpl", "Context is null");
    }
  }

  /**
   * Use {@link #initComponentHostView(Context context)} instead.
   */
  @Deprecated
  protected void initView() {
    if (mContext != null)
      mHost = initComponentHostView(mContext);
  }


  /**
   * Create corresponding view for this component.
   *
   * @param context
   * @return
   */
  protected T initComponentHostView(@NonNull Context context) {
    /**
     * compatible old initView
     * TODO: change to abstract method in next V1.0 .
     */
    return null;
  }

  /**
   * Called after host view init. <br>
   * Any overriding methods should invoke this method at the right time, to ensure the cached animation can be triggered correctly.
   * (the animation will be cached when {@link #isLazy()} is true)
   *
   * @param host the host view
   */
  @CallSuper
  protected void onHostViewInitialized(T host) {
    if (mAnimationHolder != null) {
//      Performs cached animation
      mAnimationHolder.execute(mInstance, this);
    }
    setActiveTouchListener();
  }

  public T getHostView() {
    return mHost;
  }

  /**
   * use {@link #getHostView()} instead
   *
   * @return
   */
  @Deprecated
  public View getView() {
    return mHost;
  }

  public int getAbsoluteY() {
    return mAbsoluteY;
  }

  public int getAbsoluteX() {
    return mAbsoluteX;
  }

  public final void removeEvent(String type) {
    if (TextUtils.isEmpty(type)) {
      return;
    }
    if (mEvents == null || mAppendEvents == null || mGestureType == null) {
      return;
    }
    mEvents.remove(type);
    mAppendEvents.remove(type);//only clean append events, not dom's events.
    mGestureType.remove(type);
    removeEventFromView(type);
  }

  protected void removeEventFromView(String type) {
    if (type.equals(Constants.Event.CLICK) && getRealView() != null && mHostClickListeners != null) {
      mHostClickListeners.remove(mClickEventListener);
      //click event only remove from listener array
    }
    Scrollable scroller = getParentScroller();
    if (type.equals(Constants.Event.APPEAR) && scroller != null) {
      scroller.unbindAppearEvent(this);
    }
    if (type.equals(Constants.Event.DISAPPEAR) && scroller != null) {
      scroller.unbindDisappearEvent(this);
    }
  }

  public final void removeAllEvent() {
    if (getEvents().size() < 1) {
      return;
    }
    for (String event : getEvents()) {
      if (event == null) {
        continue;
      }
      removeEventFromView(event);
    }
    mAppendEvents.clear();//only clean append events, not dom's events.
    mGestureType.clear();
    mGesture = null;
    if (getRealView() != null &&
            getRealView() instanceof WXGestureObservable) {
      ((WXGestureObservable) getRealView()).registerGestureListener(null);
    }
    if (mHost != null) {
      mHost.setOnFocusChangeListener(null);
      if (mHostClickListeners != null && mHostClickListeners.size() > 0) {
        mHostClickListeners.clear();
        mHost.setOnClickListener(null);
      }
    }
  }

  public final void removeStickyStyle() {
    if (isSticky()) {
      Scrollable scroller = getParentScroller();
      if (scroller != null) {
        scroller.unbindStickStyle(this);
      }
    }
  }

  public boolean isSticky() {
    return getStyles().isSticky();
  }

  public boolean isFixed() {
    return getStyles().isFixed();
  }

  public void setDisabled(boolean disabled) {
    mIsDisabled = disabled;
    if (mHost == null) {
      return;
    }
    mHost.setEnabled(!disabled);
  }

  public boolean isDisabled() {
    return mIsDisabled;
  }

  public void setSticky(String sticky) {
    if (!TextUtils.isEmpty(sticky) && sticky.equals(Constants.Value.STICKY)) {
      Scrollable waScroller = getParentScroller();
      if (waScroller != null) {
        waScroller.bindStickStyle(this);
      }
    }
  }

  public void setBackgroundColor(String color) {
    if (!TextUtils.isEmpty(color) && mHost != null) {
      int colorInt = WXResourceUtils.getColor(color);
      if (!(colorInt == Color.TRANSPARENT && mBackgroundDrawable == null)) {
        getOrCreateBorder().setColor(colorInt);
      }
    }
  }

  public void setBackgroundImage(@NonNull String bgImage) {
    if ("".equals(bgImage.trim())) {
      getOrCreateBorder().setImage(null);
    } else {
      Shader shader = WXResourceUtils.getShader(bgImage, mLayoutSize.getWidth(), mLayoutSize.getHeight());
      getOrCreateBorder().setImage(shader);
    }
  }

  public void setOpacity(float opacity) {
    if (opacity >= 0 && opacity <= 1 && mHost.getAlpha() != opacity) {
      if (isLayerTypeEnabled()) {
        mHost.setLayerType(View.LAYER_TYPE_HARDWARE, null);
      }
      mHost.setAlpha(opacity);
    }
  }

  public void setBorderRadius(String key, float borderRadius) {
    if (borderRadius >= 0) {
      switch (key) {
        case Constants.Name.BORDER_RADIUS:
          getOrCreateBorder().setBorderRadius(BorderDrawable.BORDER_RADIUS_ALL, WXViewUtils.getRealSubPxByWidth(borderRadius, mInstance.getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_TOP_LEFT_RADIUS:
          getOrCreateBorder().setBorderRadius(BorderDrawable.BORDER_TOP_LEFT_RADIUS, WXViewUtils.getRealSubPxByWidth(borderRadius, mInstance.getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_TOP_RIGHT_RADIUS:
          getOrCreateBorder().setBorderRadius(BorderDrawable.BORDER_TOP_RIGHT_RADIUS, WXViewUtils.getRealSubPxByWidth(borderRadius, mInstance.getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_BOTTOM_RIGHT_RADIUS:
          getOrCreateBorder().setBorderRadius(BorderDrawable.BORDER_BOTTOM_RIGHT_RADIUS, WXViewUtils.getRealSubPxByWidth(borderRadius, mInstance.getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_BOTTOM_LEFT_RADIUS:
          getOrCreateBorder().setBorderRadius(BorderDrawable.BORDER_BOTTOM_LEFT_RADIUS, WXViewUtils.getRealSubPxByWidth(borderRadius, mInstance.getInstanceViewPortWidth()));
          break;
      }
    }
  }

  public void setBorderWidth(String key, float borderWidth) {
    if (borderWidth >= 0) {
      switch (key) {
        case Constants.Name.BORDER_WIDTH:
          getOrCreateBorder().setBorderWidth(Spacing.ALL, WXViewUtils.getRealSubPxByWidth(borderWidth, getInstance().getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_TOP_WIDTH:
          getOrCreateBorder().setBorderWidth(Spacing.TOP, WXViewUtils.getRealSubPxByWidth(borderWidth, getInstance().getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_RIGHT_WIDTH:
          getOrCreateBorder().setBorderWidth(Spacing.RIGHT, WXViewUtils.getRealSubPxByWidth(borderWidth, getInstance().getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_BOTTOM_WIDTH:
          getOrCreateBorder().setBorderWidth(Spacing.BOTTOM, WXViewUtils.getRealSubPxByWidth(borderWidth, getInstance().getInstanceViewPortWidth()));
          break;
        case Constants.Name.BORDER_LEFT_WIDTH:
          getOrCreateBorder().setBorderWidth(Spacing.LEFT, WXViewUtils.getRealSubPxByWidth(borderWidth, getInstance().getInstanceViewPortWidth()));
          break;
      }
    }
  }

  public void setBorderStyle(String key, String borderStyle) {
    if (!TextUtils.isEmpty(borderStyle)) {
      switch (key) {
        case Constants.Name.BORDER_STYLE:
          getOrCreateBorder().setBorderStyle(Spacing.ALL, borderStyle);
          break;
        case Constants.Name.BORDER_RIGHT_STYLE:
          getOrCreateBorder().setBorderStyle(Spacing.RIGHT, borderStyle);
          break;
        case Constants.Name.BORDER_BOTTOM_STYLE:
          getOrCreateBorder().setBorderStyle(Spacing.BOTTOM, borderStyle);
          break;
        case Constants.Name.BORDER_LEFT_STYLE:
          getOrCreateBorder().setBorderStyle(Spacing.LEFT, borderStyle);
          break;
        case Constants.Name.BORDER_TOP_STYLE:
          getOrCreateBorder().setBorderStyle(Spacing.TOP, borderStyle);
          break;
      }
    }
  }

  public void setBorderColor(String key, String borderColor) {
    if (!TextUtils.isEmpty(borderColor)) {
      int colorInt = WXResourceUtils.getColor(borderColor);
      if (colorInt != Integer.MIN_VALUE) {
        switch (key) {
          case Constants.Name.BORDER_COLOR:
            getOrCreateBorder().setBorderColor(Spacing.ALL, colorInt);
            break;
          case Constants.Name.BORDER_TOP_COLOR:
            getOrCreateBorder().setBorderColor(Spacing.TOP, colorInt);
            break;
          case Constants.Name.BORDER_RIGHT_COLOR:
            getOrCreateBorder().setBorderColor(Spacing.RIGHT, colorInt);
            break;
          case Constants.Name.BORDER_BOTTOM_COLOR:
            getOrCreateBorder().setBorderColor(Spacing.BOTTOM, colorInt);
            break;
          case Constants.Name.BORDER_LEFT_COLOR:
            getOrCreateBorder().setBorderColor(Spacing.LEFT, colorInt);
            break;
        }
      }
    }
  }

  public
  @Nullable
  String getVisibility() {
    try {
      return (String) getStyles().get(Constants.Name.VISIBILITY);
    } catch (Exception e) {
      return Constants.Value.VISIBLE;
    }
  }

  public void setVisibility(String visibility) {
    View view;
    if ((view = getRealView()) != null) {
      if (TextUtils.equals(visibility, Constants.Value.VISIBLE)) {
        view.setVisibility(View.VISIBLE);
      } else if (TextUtils.equals(visibility, Constants.Value.HIDDEN)) {
        view.setVisibility(View.GONE);
      }
    }
  }

  /**
   * This is an experimental feature for elevation of material design.
   */
  private void updateElevation() {
    float elevation = getAttrs().getElevation(getInstance().getInstanceViewPortWidth());
    if (!Float.isNaN(elevation)) {
      ViewCompat.setElevation(getHostView(), elevation);
    }
  }

  @Deprecated
  public void registerActivityStateListener() {

  }


  /********************************
   *  begin hook Activity life cycle callback
   ********************************************************/
  public void onActivityCreate() {

  }

  public void onActivityStart() {

  }

  public void onActivityPause() {

  }

  public void onActivityResume() {

  }

  public void onActivityStop() {

  }

  public void onActivityDestroy() {

  }

  public boolean onActivityBack() {
    return false;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {

  }

  public boolean onCreateOptionsMenu(Menu menu) {
    return false;
  }

  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

  }

  /********************************
   *  end hook Activity life cycle callback
   ********************************************************/
  public void recycled() {
    if (isFixed())
      return;
  }

  public void destroy() {
    ComponentObserver observer;
    if ((observer = getInstance().getComponentObserver()) != null) {
      observer.onPreDestory(this);
    }

    if (WXEnvironment.isApkDebugable() && !WXUtils.isUiThread()) {
      throw new WXRuntimeException("[WXComponent] destroy can only be called in main thread");
    }
    if (mHost != null && mHost.getLayerType() == View.LAYER_TYPE_HARDWARE && isLayerTypeEnabled()) {
      mHost.setLayerType(View.LAYER_TYPE_NONE, null);
    }
    removeAllEvent();
    removeStickyStyle();

    View view;
    if (isFixed() && (view = getHostView()) != null) {
      getInstance().removeFixedView(view);
    }

    mIsDestroyed = true;
  }

  public boolean isDestoryed() {
    return mIsDestroyed;
  }

  /**
   * Detach view from its component. Components,
   * which have difference between getHostView and getRealView or have temp calculation results,
   * must<strong> override</strong>  this method with their own implementation.
   *
   * @return the original View
   */
  public View detachViewAndClearPreInfo() {
    View original = mHost;
    mPreRealLeft = 0;
    mPreRealWidth = 0;
    mPreRealHeight = 0;
    mPreRealTop = 0;
//    mHost = null;
    return original;
  }

  /**
   * This method computes user visible left-top point in view's coordinate.
   * The default implementation uses the scrollX and scrollY of the view as the result,
   * and put the value in the parameter pointer.
   * Components with different computation algorithm
   * <strong> should override </strong> this method.
   *
   * @param pointF the user visible left-top point in view's coordinate.
   */
  public void computeVisiblePointInViewCoordinate(PointF pointF) {
    View view = getRealView();
    pointF.set(view.getScrollX(), view.getScrollY());
  }

  public boolean containsGesture(WXGestureType WXGestureType) {
    return mGestureType != null && mGestureType.contains(WXGestureType.toString());
  }

  protected boolean containsEvent(String event) {
    return getEvents().contains(event) || mAppendEvents.contains(event);
  }

  public void notifyAppearStateChange(String wxEventType, String direction) {
    if (containsEvent(Constants.Event.APPEAR) || containsEvent(Constants.Event.DISAPPEAR)) {
      Map<String, Object> params = new HashMap<>();
      params.put("direction", direction);
      fireEvent(wxEventType, params);
    }
  }

  public boolean isUsing() {
    return isUsing;
  }

  public void setUsing(boolean using) {
    isUsing = using;
  }

  public void readyToRender() {
    if (mParent != null && getInstance().isTrackComponent()) {
      mParent.readyToRender();
    }
  }

  public static class MeasureOutput {

    public int width;
    public int height;
  }

  /**
   * Determine whether the current component needs to be placed in the real View tree
   *
   * @return false component add subview
   */
  public boolean isVirtualComponent() {
    return mType == TYPE_VIRTUAL;
  }

  public void removeVirtualComponent() {
  }

  public void setType(int type) {
    mType = type;
  }

  public int getType() {
    return mType;
  }

  public boolean hasScrollParent(WXComponent component) {
    if (component.getParent() == null) {
      return true;
    } else if (component.getParent() instanceof WXScroller) {
      return false;
    } else {
      return hasScrollParent(component.getParent());
    }
  }

  /**
   * Called when property has empty value
   *
   * @param propName
   */
  @CheckResult
  protected Object convertEmptyProperty(String propName, Object originalValue) {
    switch (propName) {
      case Constants.Name.BACKGROUND_COLOR:
        return "transparent";
      case Constants.Name.BORDER_RADIUS:
      case Constants.Name.BORDER_BOTTOM_LEFT_RADIUS:
      case Constants.Name.BORDER_BOTTOM_RIGHT_RADIUS:
      case Constants.Name.BORDER_TOP_LEFT_RADIUS:
      case Constants.Name.BORDER_TOP_RIGHT_RADIUS:
        return 0;
      case Constants.Name.BORDER_WIDTH:
      case Constants.Name.BORDER_TOP_WIDTH:
      case Constants.Name.BORDER_LEFT_WIDTH:
      case Constants.Name.BORDER_RIGHT_WIDTH:
      case Constants.Name.BORDER_BOTTOM_WIDTH:
        return 0;
      case Constants.Name.BORDER_COLOR:
      case Constants.Name.BORDER_TOP_COLOR:
      case Constants.Name.BORDER_LEFT_COLOR:
      case Constants.Name.BORDER_RIGHT_COLOR:
      case Constants.Name.BORDER_BOTTOM_COLOR:
        return "black";
    }
    return originalValue;
  }

  private void setActiveTouchListener() {
    boolean hasActivePesudo = getStyles().getPesudoStyles().containsKey(Constants.PSEUDO.ACTIVE);
    View view;
    if (hasActivePesudo && (view = getRealView()) != null) {
      boolean hasTouchConsumer = isConsumeTouch();
      view.setOnTouchListener(new TouchActivePseudoListener(this, !hasTouchConsumer));
    }
  }

  protected boolean isConsumeTouch() {
    return (mHostClickListeners != null && mHostClickListeners.size() > 0) || mGesture != null;
  }

  @Override
  public void updateActivePseudo(boolean isSet) {
    setPseudoClassStatus(Constants.PSEUDO.ACTIVE, isSet);
  }

  /**
   * @param clzName like ':active' or ':active:enabled'
   * @param status
   */
  protected void setPseudoClassStatus(String clzName, boolean status) {
    WXStyle styles = getStyles();
    Map<String, Map<String, Object>> pesudoStyles = styles.getPesudoStyles();

    if (pesudoStyles == null || pesudoStyles.size() == 0) {
      return;
    }
    Map<String, Object> resultStyles = mPesudoStatus.updateStatusAndGetUpdateStyles(
            clzName,
            status,
            pesudoStyles,
            styles.getPesudoResetStyles());
    updateStyleByPesudo(resultStyles);
  }

  private void updateStyleByPesudo(Map<String, Object> styles) {
    Message message = Message.obtain();
    WXDomTask task = new WXDomTask();
    task.instanceId = getInstanceId();
    task.args = new ArrayList<>();

    JSONObject styleJson = new JSONObject(styles);
    task.args.add(getRef());
    task.args.add(styleJson);
    task.args.add(true);//flag pesudo
    message.obj = task;
    message.what = WXDomHandler.MsgType.WX_DOM_UPDATE_STYLE;
    WXSDKManager.getInstance().getWXDomManager().sendMessage(message);
  }

  public int getStickyOffset() {
    return mStickyOffset;
  }

  public boolean canRecycled() {
    return (!isFixed() || !isSticky()) && getAttrs().canRecycled();
  }

  /**
   * Sets the offset for the sticky
   *
   * @param stickyOffset child[y]-parent[y]
   */
  public void setStickyOffset(int stickyOffset) {
    mStickyOffset = stickyOffset;
  }

  /**
   * For now, this method respect the result of {@link WXSDKInstance#isLayerTypeEnabled()}
   *
   * @return Refer {@link WXSDKInstance#isLayerTypeEnabled()}
   */
  public boolean isLayerTypeEnabled() {
    return getInstance().isLayerTypeEnabled();
  }

  /**
   * Sets whether or not to relayout page during animation, default is false
   */
  public void setNeedLayoutOnAnimation(boolean need) {
    this.mNeedLayoutOnAnimation = need;
  }

  /**
   * Trigger a applyStyles invoke to relayout current page
   */
  public void notifyNativeSizeChanged(int w, int h) {
    if (!mNeedLayoutOnAnimation) {
      return;
    }

    Message message = Message.obtain();
    WXDomTask task = new WXDomTask();
    task.instanceId = getInstanceId();
    if (task.args == null) {
      task.args = new ArrayList<>();
    }

    JSONObject style = new JSONObject(2);
    float webW = WXViewUtils.getWebPxByWidth(w);
    float webH = WXViewUtils.getWebPxByWidth(h);

    style.put("width", webW);
    style.put("height", webH);

    task.args.add(getRef());
    task.args.add(style);
    message.obj = task;
    message.what = WXDomHandler.MsgType.WX_DOM_UPDATE_STYLE;
    WXSDKManager.getInstance().getWXDomManager().sendMessage(message);
  }

  public int getViewPortWidth() {
    return mViewPortWidth;
  }

  public void setViewPortWidth(int mViewPortWidth) {
    this.mViewPortWidth = mViewPortWidth;
  }

  public WXUISize getLayoutSize() {
    return mLayoutSize;
  }

  public WXUIPosition getLayoutPosition() {
    return mLayoutPosition;
  }

  public float getLayoutX() {
    return mLayoutPosition.getLeft();
  }

  public float getLayoutY() {
    return mLayoutPosition.getTop();
  }

  public Object getExtra() {
    return null;
  }

  public String getComponentType() {
    return mComponentType;
  }

  public String getPageId() {
    return mPageId;
  }
}
