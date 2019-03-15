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
package com.taobao.weex.ui.action;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.common.Constants;
import com.taobao.weex.utils.FontDO;
import com.taobao.weex.utils.TypefaceUtil;

/**
 * Created by listen on 18/01/10.
 */
public class ActionAddRule implements IExecutable {

  private final String mPageId;
  private final String mType;
  private final JSONObject mData;

  public ActionAddRule(String pageId, String type, JSONObject data) {
    this.mPageId = pageId;
    this.mType = type;
    this.mData = data;
  }

  @Override
  public void executeAction() {
    WXSDKInstance instance = WXSDKManager.getInstance().getWXRenderManager().getWXSDKInstance(mPageId);
    if (instance == null || instance.isDestroy()) {
      return;
    }

    if (!Constants.Name.FONT_FACE.equals(mType)) {
      return;
    }

    FontDO fontDO = parseFontDO(mData, instance);
    if (fontDO != null && !TextUtils.isEmpty(fontDO.getFontFamilyName())) {
      notifyAddFontRule(instance, fontDO);
      FontDO cacheFontDO = TypefaceUtil.getFontDO(fontDO.getFontFamilyName());
      if (cacheFontDO == null || !TextUtils.equals(cacheFontDO.getUrl(), fontDO.getUrl())) {
        TypefaceUtil.putFontDO(fontDO);
        TypefaceUtil.loadTypeface(fontDO, true);
      } else {
        TypefaceUtil.loadTypeface(cacheFontDO, true);
      }
    }

  }

  private FontDO parseFontDO(JSONObject jsonObject,WXSDKInstance instance) {
    if(jsonObject == null) {
      return null;
    }
    String src = jsonObject.getString(Constants.Name.SRC);
    String name = jsonObject.getString(Constants.Name.FONT_FAMILY);

    return new FontDO(name, src,instance);
  }


  private void notifyAddFontRule(WXSDKInstance instance, FontDO fontDO){
    Intent intent = new Intent(ACTION_WEEX_ADD_RULE_FONT);
    intent.putExtra(FONT_FAMILY_NAME, fontDO.getFontFamilyName());
    intent.putExtra(FONT_URL, fontDO.getUrl());
    intent.putExtra(PAGE_ID, instance.getInstanceId());
    LocalBroadcastManager.getInstance(instance.getContext()).sendBroadcast(intent);
  }

  /**
   * Keep The Same With Render FontManager's Constants.
   * */
  public static final String ACTION_WEEX_ADD_RULE_FONT = "ACTION_WEEX_ADD_RULE_FONT";
  public static final String FONT_FAMILY_NAME = "fontFamily";
  public static final String FONT_URL = "fontUrl";
  public static final String PAGE_ID = "pageId";
}
