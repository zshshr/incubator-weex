#ifndef RenderObject_h
#define RenderObject_h

#include <vector>
#include <string>
#include <map>
#include <set>
#include <Layout/ConstantsName.h>
#include <Layout/CssValueGetter.h>
#include <Layout/WXCoreLayout.h>
#include <Layout/WXCoreStyle.h>
#include <base/android/string/StringUtils.h>
#include <base/android/LogUtils.h>

namespace WeexCore {

  class RenderObject;

  class RenderPage;

  typedef std::map<std::string, std::string>::const_iterator STYLE_IT;
  typedef std::map<std::string, std::string>::const_iterator ATTR_IT;
  typedef std::set<std::string>::const_iterator EVENT_IT;
  typedef std::map<std::string, std::string> STYLES_MAP;
  typedef std::map<std::string, std::string> ATTRIBUTES_MAP;
  typedef std::set<std::string> EVENTS_SET;
  typedef std::map<std::string, std::string> MARGIN_MAP;
  typedef std::map<std::string, std::string> PADDING_MAP;
  typedef std::map<std::string, std::string> BORDER_MAP;

  class RenderObject : public WXCoreLayoutNode {
  public:

  private:
    std::string mRef = "";

    std::string mType = "";

    RenderPage *mPage;

    RenderObject *mParentRender;

    STYLES_MAP *mStyles;

    ATTRIBUTES_MAP *mAttributes;

    MARGIN_MAP *mMargins;

    PADDING_MAP *mPaddings;

    BORDER_MAP *mBorders;

    EVENTS_SET *mEvents;

    jobject mComponent_android;

  public:

    RenderObject(RenderPage *page);

    ~RenderObject();

    inline void addRenderObject(int index, RenderObject *child) {
      // insert RenderObject child
      addChildAt(child, getChildCount());
    }

    inline void removeRenderObject(RenderObject *child) {
      removeChild(child);
    }

    inline void updateAttr(std::string key, std::string value) {
      mAttributes->insert(pair<std::string, std::string>(key, value));
    }

    inline void updateStyle(std::string key, std::string value) {
      applyStyle(key, value);
    }

    inline void addEvent(std::string event) {
      mEvents->insert(event);
    }

    inline void removeEvent(std::string event) {
      mEvents->erase(event);
    }

    inline void setRef(std::string ref) {
      mRef = ref;
    }

    inline std::string getRef() {
      return mRef;
    }

    inline void setType(std::string type) {
      mType = type;
    }

    inline std::string getType() {
      return mType;
    }

    inline void setParentRender(RenderObject *render) {
      mParentRender = render;
    }

    inline RenderObject *getParentRender() {
      return mParentRender;
    }

    inline STYLES_MAP *getStyles() {
      return mStyles;
    }

    inline ATTRIBUTES_MAP *getAttributes() {
      return mAttributes;
    }

    inline EVENTS_SET *getEvents() {
      return mEvents;
    }

    inline PADDING_MAP *getPaddings() {
      return mPaddings;
    }

    inline MARGIN_MAP *getMargins() {
      return mMargins;
    }

    inline BORDER_MAP *getBorders() {
      return mBorders;
    }

    inline STYLE_IT getStyleItBegin() {
      return mStyles->begin();
    }

    inline STYLE_IT getStyleItEnd() {
      return mStyles->end();
    }

    inline ATTR_IT getAttrItBegin() {
      return mAttributes->begin();
    }

    inline ATTR_IT getAttrItEnd() {
      return mAttributes->end();
    }

    inline EVENT_IT getEventItBegin() {
      return mEvents->begin();
    }

    inline EVENT_IT getEventItEnd() {
      return mEvents->end();
    }

    void applyStyle(std::string key, std::string value);

    void printRenderMsg();

    void printYGNodeMsg();

    inline void bindComponent_Impl_Android(jobject component) {
      this->mComponent_android = component;
    }

    inline RenderObject* getChild(uint32_t index) {
      return (RenderObject *) getChildAt(index);
    }
  };
} //end WeexCore
#endif //RenderObject_h
