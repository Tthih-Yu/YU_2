public package com.suda.yzune.wakeupschedule.schedule_import;

import OooOo0O.o000OO;
import Oooo0oO.o00O00o0;
import OoooOoO.o00O00OO;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.lifecycle.o000O00;
import androidx.lifecycle.o000O0Oo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suda.yzune.wakeupschedule.R;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.WeakHashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import kotlin.LazyThreadSafetyMode;
import kotlin.Pair;
import kotlin.jvm.internal.Ref$IntRef;
import kotlin.text.Regex;
import o00OoOoo.o00O0000;

/* loaded from: classes.dex */
public final class WebViewLoginFragment extends com.suda.yzune.wakeupschedule.base_view.OooO00o {

    /* renamed from: Ooooo00, reason: collision with root package name */
    public String f8236Ooooo00;

    /* renamed from: Ooooo0o, reason: collision with root package name */
    public o00O00OO.OooO0o f8237Ooooo0o;

    /* renamed from: OooooO0, reason: collision with root package name */
    public androidx.appcompat.app.OooOOO f8238OooooO0;

    /* renamed from: OooooOO, reason: collision with root package name */
    public final o000OO f8239OooooOO;

    /* renamed from: OooooOo, reason: collision with root package name */
    public boolean f8240OooooOo;

    /* renamed from: Oooooo, reason: collision with root package name */
    public final okhttp3.o0OO00O f8241Oooooo;

    /* renamed from: Oooooo0, reason: collision with root package name */
    public final Regex f8242Oooooo0 = new Regex("(http|https)://.*?/");

    /* renamed from: OoooooO, reason: collision with root package name */
    public int f8243OoooooO;

    /* renamed from: Ooooooo, reason: collision with root package name */
    public boolean f8244Ooooooo;

    /* renamed from: o00O0O, reason: collision with root package name */
    public final Object f8245o00O0O;

    /* renamed from: o00Oo0, reason: collision with root package name */
    public int f8246o00Oo0;

    /* renamed from: o00Ooo, reason: collision with root package name */
    public final String f8247o00Ooo;

    /* renamed from: o00o0O, reason: collision with root package name */
    public final String f8248o00o0O;

    /* renamed from: o0OoOo0, reason: collision with root package name */
    public final Object f8249o0OoOo0;

    /* renamed from: ooOO, reason: collision with root package name */
    public final Object f8250ooOO;

    public final class InJavaScriptLocalObj {
        public InJavaScriptLocalObj() {
        }

        @JavascriptInterface
        public final void showSource(String html) {
            kotlin.jvm.internal.OooOO0O.OooO0o0(html, "html");
            WebViewLoginFragment webViewLoginFragment = WebViewLoginFragment.this;
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "login_chaoxing")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$1(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "zju_post")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$2(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "cumtb") || kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "jmu")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$3(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "ahmu")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$4(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "ccibe")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$5(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "cppu")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$6(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "ucas")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$7(webViewLoginFragment, html, null));
                return;
            }
            if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "chaoxing_share")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$8(webViewLoginFragment, html, null));
            } else if (kotlin.jvm.internal.OooOO0O.OooO00o(webViewLoginFragment.OoooOO0().f8195OooO0oO, "apply")) {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$10(webViewLoginFragment, html, null));
            } else {
                com.suda.yzune.wakeupschedule.base_view.OooO00o.OoooO0(webViewLoginFragment, new WebViewLoginFragment$InJavaScriptLocalObj$showSource$9(webViewLoginFragment, html, null));
            }
        }
    }

    public WebViewLoginFragment() {
        final int i = 0;
        final o00OOO00.OooO00o oooO00o = null;
        this.f8239OooooOO = o000ooo.oo0o0Oo.OooOOO0(this, kotlin.jvm.internal.OooOOO.OooO00o(OooOo00.class), new o00OOO00.OooO00o() { // from class: com.suda.yzune.wakeupschedule.schedule_import.WebViewLoginFragment$special$$inlined$activityViewModels$default$1
            {
                super(0);
            }

            @Override // o00OOO00.OooO00o
            public final o000O0Oo invoke() {
                return com.huawei.hms.aaid.utils.OooO00o.OooOOO0(androidx.fragment.app.o0OO00O.this, "requireActivity().viewModelStore");
            }
        }, new o00OOO00.OooO00o() { // from class: com.suda.yzune.wakeupschedule.schedule_import.WebViewLoginFragment$special$$inlined$activityViewModels$default$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(0);
            }

            @Override // o00OOO00.OooO00o
            public final o00O00OO invoke() {
                o00O00OO o00o00oo2;
                o00OOO00.OooO00o oooO00o2 = o00OOO00.OooO00o.this;
                return (oooO00o2 == null || (o00o00oo2 = (o00O00OO) oooO00o2.invoke()) == null) ? this.Oooo0O0().OooO0OO() : o00o00oo2;
            }
        }, new o00OOO00.OooO00o() { // from class: com.suda.yzune.wakeupschedule.schedule_import.WebViewLoginFragment$special$$inlined$activityViewModels$default$3
            {
                super(0);
            }

            @Override // o00OOO00.OooO00o
            public final o000O00 invoke() {
                return com.huawei.hms.aaid.utils.OooO00o.OooOO0o(androidx.fragment.app.o0OO00O.this, "requireActivity().defaultViewModelProviderFactory");
            }
        });
        okhttp3.o0Oo0oo o0oo0oo = new okhttp3.o0Oo0oo();
        o0oo0oo.f12876OooO = false;
        o0oo0oo.f12883OooO0oo = false;
        SSLContext sSLContext = SSLContext.getInstance("TLS");
        sSLContext.init(null, new TrustManager[]{new com.suda.yzune.wakeupschedule.utils.o000oOoO()}, new SecureRandom());
        SSLSocketFactory socketFactory = sSLContext.getSocketFactory();
        kotlin.jvm.internal.OooOO0O.OooO0Oo(socketFactory, "getSocketFactory(...)");
        o0oo0oo.OooO0O0(socketFactory, new com.suda.yzune.wakeupschedule.utils.o000oOoO());
        this.f8241Oooooo = new okhttp3.o0OO00O(o0oo0oo);
        LazyThreadSafetyMode lazyThreadSafetyMode = LazyThreadSafetyMode.NONE;
        this.f8249o0OoOo0 = kotlin.OooO00o.OooO00o(lazyThreadSafetyMode, new o00OOO00.OooO00o(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o00000O0

            /* renamed from: OooO0oo, reason: collision with root package name */
            public final /* synthetic */ WebViewLoginFragment f8263OooO0oo;

            {
                this.f8263OooO0oo = this;
            }

            /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.Object, java.util.Map] */
            @Override // o00OOO00.OooO00o
            public final Object invoke() {
                switch (i) {
                    case 0:
                        InputStream open = this.f8263OooO0oo.OooOOO0().getAssets().open("set_meta.txt");
                        kotlin.jvm.internal.OooOO0O.OooO0Oo(open, "open(...)");
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open, kotlin.text.OooO00o.f9014OooO00o), 8192);
                        try {
                            String OooO0Oo = com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0Oo(com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0o0(kotlin.text.o00Ooo.o0000Ooo(o000ooo.oo0o0Oo.Oooo(bufferedReader)).toString()));
                            com.bumptech.glide.OooO0o.OooO(bufferedReader, null);
                            return OooO0Oo;
                        } finally {
                        }
                    default:
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                        WebViewLoginFragment webViewLoginFragment = this.f8263OooO0oo;
                        ?? r2 = webViewLoginFragment.f8250ooOO;
                        String str = (String) r2.get(webViewLoginFragment.OoooOO0().f8193OooO0o);
                        int i2 = 0;
                        if (str == null && (str = (String) r2.get(webViewLoginFragment.OoooOO0().f8195OooO0oO)) == null) {
                            String str2 = webViewLoginFragment.OoooOO0().f8195OooO0oO;
                            if (str2 == null || !kotlin.text.o0O0O00.o00Ooo(str2, "qz", false)) {
                                String str3 = webViewLoginFragment.OoooOO0().f8195OooO0oO;
                                str = (str3 == null || !kotlin.text.o00Ooo.o00o0O(str3, "shuwei", false)) ? (String) r2.get("default") : (String) r2.get("shuwei");
                            } else {
                                str = (String) r2.get("qz");
                            }
                        }
                        if (str != null) {
                            for (Object obj : kotlin.text.o00Ooo.o0000O00(str, new String[]{"<>"})) {
                                int i3 = i2 + 1;
                                if (i2 < 0) {
                                    o00OO00o.o00000O0.Oooooo0();
                                    throw null;
                                }
                                String str4 = (String) obj;
                                if (i2 % 2 == 1) {
                                    Context OooOO02 = webViewLoginFragment.OooOO0();
                                    kotlin.jvm.internal.OooOO0O.OooO0O0(OooOO02);
                                    spannableStringBuilder.append(str4, new ForegroundColorSpan(o00o0O00.OooOOOO.OooO0OO(OooOO02, R.attr.colorError)), 33);
                                } else {
                                    spannableStringBuilder.append((CharSequence) str4);
                                }
                                i2 = i3;
                            }
                        }
                        return spannableStringBuilder;
                }
            }
        });
        this.f8250ooOO = kotlin.collections.OooO00o.OooooOo(new Pair("default", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>个人课表<>的页面，注意选择自己需要导入的学期，<>一般首页的课表都是不可导入的！<>另外<>不会导入调课、停课的信息<>，请导入后自行修改！\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("apply", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到个人课表或者相关的页面。\n3. 点击右下角的按钮抓取源码，并上传到服务器。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("qz", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>「学期理论课表」<>的页面，注意<>不是「首页的课表」<>！注意选择自己需要导入的学期。\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("jz", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>「个人课表」<>的页面（如「信息查询」->「学生个人课表」），注意<>不是「班级课表」<>！注意选择自己需要导入的学期，注意<>切换到「学期课表」<>再导入，周课表不可导入！另外<>不会导入调课、停课的信息<>，请导入后自行修改！\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("zf", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>课表<>页面，注意选择自己需要导入的学期，<>一般首页的课表都是不可导入的！<>另外<>不会导入调课、停课的信息<>，请导入后自行修改！\n3. 课表上<>要有上课时间、教室、老师等信息才能成功导入！<>如果没有的话，请去教务网的设置（或者网页课表左上角齿轮）那里，设置显示上课时间、教室、老师信息。\n4. 点击右下角的按钮完成导入。\n5. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("umooc", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录教务后，要选择<>小节课表<>，也就是「第1节」「第2节」分开显示那种。<>不支持<>导入大节课表。\n3. 点击右下角的按钮完成导入。部分学校有中午的课时，导入后会<>当成一节<>来处理。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("西北农林科技大学", "1. 可能会遇到一直登录不上的问题，<>解决方法<>：在上方输入网址处，最后面的斜杠加上hhh，然后登录后会跳转到一个错误页面，再把hhh删掉，按→或回车，就能进入登录后的页面。\n2. 登录后点击到<>「个人课表」<>的页面（如「信息查询」->「学生个人课表」），注意<>不是「班级课表」<>！\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("赣南医学院", "1. 选择个人课表后，选择<>全部周数<>\n2. 然后点<>「图形」<>模式\n3. 勾选<>「放大」<>\n4. 严格按照以上步骤很重要，否则可能只能导入某一周的课表\n5. 然后点右下角按钮进行导入。\n6. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("cqu", "1. 登录后点<>左上角三条杠<>，选择<>「我的课表」<>。\n2. 能导入的页面是<>「我的课表」<>，不是选课管理。\n3. 点右下角按钮进行导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("sysu", "1. 可能需要校园网或校园 VPN 才能打开网页。\n2. 登录教务后，<>首页的课表是不可导入的<>，请打开到类似<>「查询课表」<>的含有<>全部周<>课程的页面。\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("gdbyxy", "1. 登录教务后，选择左栏“教学安排”，然后点“<>教学安排表<>”，学年学期选择自己要导入的学期，格式要选择<>「格式一」<>！然后点检索。\n2. 点击右下角的按钮完成导入。\n3. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("kg_zx", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录教务后，要选择「<>网上选课<>」->「<>正选结果<>」。\n3. 如果「正选结果」打不开或无数据，则是<>无法导入的青果教务<>，请尝试其他方式添加课程。"), new Pair("kingo_new", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录教务后，要选择「<>主控<>」->「<>教学安排<>」或者「<>班级课表<>」->「<>格式二<>」，然后点教务上的<>检索<>按钮，<>而不是导出、打印按钮<>！\n3. 如果「教学安排」打不开或无数据或者是<>图片格式的课表<>，则是<>无法导入的青果教务<>，请尝试其他方式添加课程。"), new Pair("hust", "1. 登录后请选择<>「按课程」<>显示课表。\n2. 点网页上的查询，然后点右下角按钮进行导入。\n3. 时间地点为<>「待定」的课程不会导入<>，请后续手动添加。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("urp", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>本学期课表<>的页面，目前只能导入本学期课表，<>历年学期课表页面是不可导入的！<>\n3. 点击右下角的按钮完成导入。"), new Pair("urp_new", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>本学期课表<>的页面，目前只能导入本学期课表，<>历年学期课表页面是不可导入的！<>\n3. 点击右下角的按钮完成导入。"), new Pair("urp_new_ajax", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>本学期课表<>的页面，目前只能导入本学期课表，<>历年学期课表页面是不可导入的！<>\n3. 点击右下角的按钮完成导入。"), new Pair("jnu", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录教务后操作：<>左边导航栏->选课管理系统->课程表及考试表<>。\n3. 点击右下角的按钮完成导入，要进行<>多次操作<>，请耐心等待网页加载。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("shuwei", "1. 要在课表页面才能导入，加载速度可能有点慢\n2. 如果<>页面显示有问题<>，可以尝试点右下角导入按钮\n3. 一定要<>等课表页面加载完成<>再点导入！"), new Pair("shuwei_new", "1. 要在<>「我的课表」<>页面才能导入，加载速度可能有点慢\n2. 如果<>页面显示有问题<>，可以尝试点右下角导入按钮\n3. 一定要<>等课表页面加载完成<>再点导入！"), new Pair("south_soft", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>培养管理 → 学生课表查询<>的页面，注意选择自己需要导入的学期！\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("jxau", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>课表查询 → 本人课表查询 → 打印传统课表<>的页面。\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("bfa", "1. 登录后点击<>修读课程查询 → 学期修读课程<>的页面，查询想导入的学期。\n2. 注意<>不是「本学期分周课表」！<>\n3. 点击右下角的按钮完成导入。"), new Pair("gxnu", "1. 登录后点击<>「已选选课列表」<>进行导入。\n2. 注意<>不是「当前课程表」！<>\n3. 点击右下角的按钮完成导入。"), new Pair("cf", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>课表查询 → 我的课表<>的页面，记得<>周次要选全部<>，记得<>点查询课表<>！\n3. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("cf_new", "1. 在上方输入教务网址，部分学校需要连接校园网。\n2. 登录后点击到<>课表查询 → 我的课表<>的页面，且<>只能打开课表查询小窗，其他小窗能关则关<>！\n3. 记得<>周次要选全部<>，记得<>点查询课表<>！\n4. 点击右下角的按钮完成导入。\n4. 如果遇到网页错位等问题，可以尝试取消底栏的「电脑模式」或者调节字体缩放。"), new Pair("shtu_post", "欢迎使用上海科技大学研究生课表导入工具,本科生小朋友请出门左转用树维系统导入工具导入.\n登录后,请打开 我的培养-查看课表 再导入.如果右上角用户角色为 答辩秘书,还需要先切换为 研究生.\n1.对于研究生选修本科生课的情况,教务系统中显示的课表中可能没有课程的标题信息.\n2.对于SIST/SLST/SPST以外的其他学院开设的课程,教务系统中显示的课表中可能没有课程的标题信息.\n对于这些情况,课程标题暂且展示为班级+教师信息.\n这些问题均出自教务系统的bug,对于未有明确修正说明的情况本工具均“依样”输出.\n<>建议自行在我的培养-排课结果查询 利用教室信息查询并手动修正.<>\n如果你遇到其他问题,可以带上截图及课表页面HTML发邮件到 y@wanghy.gq ."), new Pair("chaoxing_share", "1. 这里能导入的应该是超星<>分享<>出来的课程表，<>无需登录即可查看那种<>。如果学校用的是超星教务，请返回上一页选择<>超星教务<>。2. 在上方地址栏输入分享的链接，加载完成后点右下角的导入按钮。"), new Pair("hnjm", "请复制<>微信端课表页面<>的链接到最上方地址栏，等页面加载完成后点右下角按钮导入。"), new Pair("ruc", "1. 本解析器只适用于<>微人大的“我的课程表（本+研）”页面<>，请不要在选课系统页面上使用！\n2. <>注意！！第十三节到第十四节课的上课时间在“我的课程表（本+研）”页面和教务系统有所不同；务必自行了解；这好像也是这学期刚改的；反正尽量早点去吧。<>\n3. 本解析器使用的方法不能保证结果的正确性！请务必手动检查！请务必自行设置起始周和学期长度等信息！"), new Pair("cnu", "登录教务后，在主页→全校课表→选择自己的专业→查询本学期课表→点右下角按钮导入"), new Pair("nwpu_post", "翱翔门户登录后，进入【研究生教育】应用，并依次选择【课程与成绩】->【选课结果查询】。待最终页面加载完毕后，方可点击按钮开始导入。"), new Pair("xauat_post", "在 教学与培养 -> 课表查询 导入"));
        final int i2 = 1;
        this.f8245o00O0O = kotlin.OooO00o.OooO00o(lazyThreadSafetyMode, new o00OOO00.OooO00o(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o00000O0

            /* renamed from: OooO0oo, reason: collision with root package name */
            public final /* synthetic */ WebViewLoginFragment f8263OooO0oo;

            {
                this.f8263OooO0oo = this;
            }

            /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.Object, java.util.Map] */
            @Override // o00OOO00.OooO00o
            public final Object invoke() {
                switch (i2) {
                    case 0:
                        InputStream open = this.f8263OooO0oo.OooOOO0().getAssets().open("set_meta.txt");
                        kotlin.jvm.internal.OooOO0O.OooO0Oo(open, "open(...)");
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open, kotlin.text.OooO00o.f9014OooO00o), 8192);
                        try {
                            String OooO0Oo = com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0Oo(com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0o0(kotlin.text.o00Ooo.o0000Ooo(o000ooo.oo0o0Oo.Oooo(bufferedReader)).toString()));
                            com.bumptech.glide.OooO0o.OooO(bufferedReader, null);
                            return OooO0Oo;
                        } finally {
                        }
                    default:
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                        WebViewLoginFragment webViewLoginFragment = this.f8263OooO0oo;
                        ?? r2 = webViewLoginFragment.f8250ooOO;
                        String str = (String) r2.get(webViewLoginFragment.OoooOO0().f8193OooO0o);
                        int i22 = 0;
                        if (str == null && (str = (String) r2.get(webViewLoginFragment.OoooOO0().f8195OooO0oO)) == null) {
                            String str2 = webViewLoginFragment.OoooOO0().f8195OooO0oO;
                            if (str2 == null || !kotlin.text.o0O0O00.o00Ooo(str2, "qz", false)) {
                                String str3 = webViewLoginFragment.OoooOO0().f8195OooO0oO;
                                str = (str3 == null || !kotlin.text.o00Ooo.o00o0O(str3, "shuwei", false)) ? (String) r2.get("default") : (String) r2.get("shuwei");
                            } else {
                                str = (String) r2.get("qz");
                            }
                        }
                        if (str != null) {
                            for (Object obj : kotlin.text.o00Ooo.o0000O00(str, new String[]{"<>"})) {
                                int i3 = i22 + 1;
                                if (i22 < 0) {
                                    o00OO00o.o00000O0.Oooooo0();
                                    throw null;
                                }
                                String str4 = (String) obj;
                                if (i22 % 2 == 1) {
                                    Context OooOO02 = webViewLoginFragment.OooOO0();
                                    kotlin.jvm.internal.OooOO0O.OooO0O0(OooOO02);
                                    spannableStringBuilder.append(str4, new ForegroundColorSpan(o00o0O00.OooOOOO.OooO0OO(OooOO02, R.attr.colorError)), 33);
                                } else {
                                    spannableStringBuilder.append((CharSequence) str4);
                                }
                                i22 = i3;
                            }
                        }
                        return spannableStringBuilder;
                }
            }
        });
        this.f8247o00Ooo = "javascript:function getPageHtml(dom) {\n  var iframeContent=\"\";  var frameContent=\"\";  const ifrs = dom.getElementsByTagName(\"iframe\");\n  const frs = dom.getElementsByTagName(\"frame\");\n  for (var i = 0; i < ifrs.length; i++) {\n     try {\n         iframeContent += getPageHtml(ifrs[i].contentDocument.body.parentElement);\n     } catch (error) { }\n  }\n  for (var i = 0; i < frs.length; i++) {\n     try {\n         frameContent += getPageHtml(frs[i].contentDocument.body.parentElement);\n     } catch (error) { }\n  }\n  if (dom === document) {\n    return (\n      document.getElementsByTagName(\"html\")[0].innerHTML + iframeContent + frameContent\n    );\n  }\n  return dom.innerHTML + iframeContent + frameContent;\n}\n";
        this.f8248o00o0O = "\n        function save2json() {\n            var VER = \"0.6\";\n\n            var rawdata = undefined;\n            var mode = undefined;\n\n            function check_page_allow() {\n                try {\n                    rawdata = $($(\"#frmright\")[0].contentDocument).find(\"#div-table tbody\")[0];\n                    mode = \"grad\";\n                    return true;\n                }\n                catch (error) {\n                    console.error(error);\n                }\n\n                try {\n                    rawdata = table0;\n                    mode = \"eams\";\n                    if (window.hasOwnProperty(\"unitCount\")) {\n                        rawdata.unitCount = unitCount;\n                    }\n                    return true;\n                }\n                catch (error) {\n                    console.error(error);\n                }\n\n                try {\n                    rawdata = window.table0;\n                    if (window.hasOwnProperty(\"unitCount\")) {\n                        rawdata.unitCount = unitCount;\n                    }\n                    if (typeof rawdata !== 'undefined') {\n                        mode = \"eams\";\n                        return true;\n                    }\n                }\n                catch (error) {\n                    console.error(error);\n                }\n\n                var ifrs = $(\"iframe\");\n                if (ifrs.length > 0) {\n                    for (var i = 0; i < ifrs.length; i++) {\n                        try {\n                            rawdata = ifrs[i].contentWindow.table0;\n                            if (ifrs[i].contentWindow.hasOwnProperty(\"unitCount\")) {\n                                rawdata.unitCount = ifrs[i].contentWindow.unitCount;\n                            }\n                            if (typeof rawdata !== 'undefined') {\n                                mode = \"eams\";\n                                return true;\n                            }\n                        } catch (error) {\n                            console.error(error);\n                        }\n                    }\n                }\n                \n                try {\n                    rawdata = ifrs.contentWindow.table0;\n                    if (ifrs.contentWindow.hasOwnProperty(\"unitCount\")) {\n                        rawdata.unitCount = ifrs.contentWindow.unitCount;\n                    }\n                    if (typeof rawdata !== 'undefined') {\n                        mode = \"eams\";\n                        return true;\n                    }\n                } catch (error) {\n                    console.error(error);\n                }\n\n            }\n\n            if (Boolean(window.$) && check_page_allow()) {\n\n                rawdata[\"marshalContents\"] = [];\n\n                if (mode == \"eams\") {\n                    var courseJson = JSON.stringify(rawdata);\n                    var targetStr = \"index.js');\";\n                    var afterIndex = courseJson.indexOf(targetStr);\n                    if (afterIndex != -1) {\n                        courseJson = courseJson.substring(afterIndex + targetStr.length);\n                    }\n                    return courseJson;\n                }\n                else if (mode == \"grad\")\n                    alert(\"这种类型还没有支持哦，请到App关于页面联系开发者适配\");\n\n            } else {\n                location.reload();\n                alert('加载失败，等界面刷新后，打开课表页面，再尝试导入！');\n            }\n        }\n        save2json()\n        ";
    }

    /* JADX WARN: Type inference failed for: r1v0, types: [java.lang.Object, o00OO00O.OooO] */
    public static final void OoooO0O(WebViewLoginFragment webViewLoginFragment, String str, Exception exc) {
        webViewLoginFragment.f8240OooooOo = false;
        webViewLoginFragment.f8246o00Oo0 = 0;
        ?? r1 = webViewLoginFragment.f8245o00O0O;
        CharSequence subSequence = ((SpannableStringBuilder) r1.getValue()).subSequence(0, ((SpannableStringBuilder) r1.getValue()).length());
        kotlin.jvm.internal.OooOO0O.OooO0OO(subSequence, "null cannot be cast to non-null type android.text.SpannableStringBuilder");
        SpannableStringBuilder spannableStringBuilder = (SpannableStringBuilder) subSequence;
        if (!kotlin.text.o00Ooo.o00o0O(str, "周一", false) && !kotlin.text.o00Ooo.o00o0O(str, "星期一", false)) {
            spannableStringBuilder.insert(0, (CharSequence) "导入失败>_<请认真看一下提示。此页面似乎没有课程信息。请仔细阅读导入操作提示：\n");
        } else if (exc instanceof NullPointerException) {
            spannableStringBuilder.insert(0, (CharSequence) "导入失败>_<请认真看一下提示。似乎是页面选择错误、教务选择错误，或教务没有适配导致的。部分学校更换了教务，App没有及时更新信息，请结合App教程自行判断教务类型。请仔细阅读导入操作提示：\n");
        } else {
            spannableStringBuilder.insert(0, (CharSequence) "导入失败>_<请认真看一下提示。似乎是教务没有适配好导致的。请仔细阅读导入操作提示：\n");
        }
        spannableStringBuilder.insert(spannableStringBuilder.length(), (CharSequence) ("\n详细的错误信息如下：\n" + exc.getMessage()));
        o00O0O0.OooO0OO oooO0OO = new o00O0O0.OooO0OO(webViewLoginFragment.Oooo0o0());
        oooO0OO.OooOOo0(R.string.title_tips);
        androidx.appcompat.app.OooO oooO = (androidx.appcompat.app.OooO) oooO0OO.f881OooO;
        oooO.f1941OooO0o = spannableStringBuilder;
        oooO.f1949OooOOO0 = false;
        oooO0OO.OooOOO(R.string.ok, null);
        oooO0OO.OooOO0o("如何正确选择教务？", new o000000(webViewLoginFragment, 0));
        oooO0OO.OooO0o();
    }

    @Override // androidx.fragment.app.o0OO00O
    public final View OooOo(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        kotlin.jvm.internal.OooOO0O.OooO0o0(inflater, "inflater");
        return inflater.inflate(R.layout.fragment_web_view_login, viewGroup, false);
    }

    @Override // androidx.fragment.app.o0OO00O
    public final void OooOo0o(Bundle bundle) {
        super.OooOo0o(bundle);
        Bundle bundle2 = this.f3807OooOO0o;
        if (bundle2 != null) {
            String string = bundle2.getString("url");
            kotlin.jvm.internal.OooOO0O.OooO0O0(string);
            this.f8236Ooooo00 = string;
        }
    }

    @Override // androidx.fragment.app.o0OO00O
    public final void OooOoO() {
        androidx.appcompat.app.OooOOO oooOOO = this.f8238OooooO0;
        if (oooOOO != null) {
            oooOOO.dismiss();
        }
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        o00O00OO.OooO0o oooO0o = this.f8237Ooooo0o;
        if (oooO0o == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o.f11096OooOoOO.setWebChromeClient(null);
        o00O00OO.OooO0o oooO0o2 = this.f8237Ooooo0o;
        if (oooO0o2 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o2.f11096OooOoOO.clearCache(true);
        o00O00OO.OooO0o oooO0o3 = this.f8237Ooooo0o;
        if (oooO0o3 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o3.f11096OooOoOO.clearFormData();
        o00O00OO.OooO0o oooO0o4 = this.f8237Ooooo0o;
        if (oooO0o4 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o4.f11096OooOoOO.clearHistory();
        o00O00OO.OooO0o oooO0o5 = this.f8237Ooooo0o;
        if (oooO0o5 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o5.f11096OooOoOO.removeAllViews();
        o00O00OO.OooO0o oooO0o6 = this.f8237Ooooo0o;
        if (oooO0o6 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        oooO0o6.f11096OooOoOO.destroy();
        this.f3831Oooo0O0 = true;
    }

    public final String OoooO() {
        o00O00OO.OooO0o oooO0o = this.f8237Ooooo0o;
        if (oooO0o == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        String url = oooO0o.f11096OooOoOO.getUrl();
        kotlin.jvm.internal.OooOO0O.OooO0O0(url);
        if (url.length() > 0) {
            androidx.work.oo0o0Oo.OooOo0O(url.charAt(kotlin.text.o00Ooo.o00oO0O(url)), '/', false);
        }
        o00O00OO.OooO0o oooO0o2 = this.f8237Ooooo0o;
        if (oooO0o2 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        String url2 = oooO0o2.f11096OooOoOO.getUrl();
        kotlin.jvm.internal.OooOO0O.OooO0O0(url2);
        kotlin.text.OooOo00 find$default = Regex.find$default(this.f8242Oooooo0, url2, 0, 2, null);
        if (find$default != null) {
            return ((kotlin.text.Oooo0) find$default).OooO0OO();
        }
        o00O00OO.OooO0o oooO0o3 = this.f8237Ooooo0o;
        if (oooO0o3 == null) {
            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
            throw null;
        }
        String url3 = oooO0o3.f11096OooOoOO.getUrl();
        kotlin.jvm.internal.OooOO0O.OooO0O0(url3);
        return url3;
    }

    public final OooOo00 OoooOO0() {
        return (OooOo00) this.f8239OooooOO.getValue();
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x00a2  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00f8  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public final void o000oOoO() {
        /*
            Method dump skipped, instructions count: 264
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.WebViewLoginFragment.o000oOoO():void");
    }

    @Override // androidx.fragment.app.o0OO00O, android.content.ComponentCallbacks
    public final void onConfigurationChanged(Configuration newConfig) {
        kotlin.jvm.internal.OooOO0O.OooO0o0(newConfig, "newConfig");
        this.f3831Oooo0O0 = true;
        String str = OoooOO0().f8193OooO0o;
        if ((str == null || !kotlin.text.o0O0O00.o00Ooo(str, "湖南科技大学", false)) && !kotlin.jvm.internal.OooOO0O.OooO00o(OoooOO0().f8195OooO0oO, "cqu")) {
            return;
        }
        androidx.appcompat.app.OooOOO oooOOO = this.f8238OooooO0;
        if (oooOOO != null) {
            oooOOO.dismiss();
        }
        androidx.appcompat.app.OooOOO oooOOO2 = this.f8238OooooO0;
        if (oooOOO2 != null) {
            oooOOO2.show();
        }
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /* JADX WARN: Type inference failed for: r6v53, types: [java.lang.Object, o00OO00O.OooO] */
    @Override // androidx.fragment.app.o0OO00O
    @JavascriptInterface
    public void onViewCreated(View view, Bundle bundle) {
        String str;
        String str2;
        final int i = 0;
        final int i2 = 2;
        final int i3 = 1;
        kotlin.jvm.internal.OooOO0O.OooO0o0(view, "view");
        int i4 = R.id.appbar_layout;
        LinearLayoutCompat linearLayoutCompat = (LinearLayoutCompat) o00O0000.OooooOo(view, R.id.appbar_layout);
        if (linearLayoutCompat != null) {
            i4 = R.id.btn_back;
            AppCompatImageButton appCompatImageButton = (AppCompatImageButton) o00O0000.OooooOo(view, R.id.btn_back);
            if (appCompatImageButton != null) {
                i4 = R.id.btn_url_tips;
                MaterialButton materialButton = (MaterialButton) o00O0000.OooooOo(view, R.id.btn_url_tips);
                if (materialButton != null) {
                    i4 = R.id.cg_qz;
                    ChipGroup chipGroup = (ChipGroup) o00O0000.OooooOo(view, R.id.cg_qz);
                    if (chipGroup != null) {
                        i4 = R.id.chip_mode;
                        Chip chip = (Chip) o00O0000.OooooOo(view, R.id.chip_mode);
                        if (chip != null) {
                            i4 = R.id.chip_password;
                            Chip chip2 = (Chip) o00O0000.OooooOo(view, R.id.chip_password);
                            if (chip2 != null) {
                                i4 = R.id.chip_qz1;
                                Chip chip3 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz1);
                                if (chip3 != null) {
                                    Chip chip4 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz2);
                                    if (chip4 != null) {
                                        Chip chip5 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz3);
                                        if (chip5 != null) {
                                            Chip chip6 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz4);
                                            if (chip6 != null) {
                                                Chip chip7 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz5);
                                                if (chip7 != null) {
                                                    Chip chip8 = (Chip) o00O0000.OooooOo(view, R.id.chip_qz6);
                                                    if (chip8 != null) {
                                                        MaterialCardView materialCardView = (MaterialCardView) o00O0000.OooooOo(view, R.id.cv_url_tips);
                                                        if (materialCardView != null) {
                                                            TextInputEditText textInputEditText = (TextInputEditText) o00O0000.OooooOo(view, R.id.et_url);
                                                            if (textInputEditText != null) {
                                                                FloatingActionButton floatingActionButton = (FloatingActionButton) o00O0000.OooooOo(view, R.id.fab_import);
                                                                if (floatingActionButton != null) {
                                                                    FloatingActionButton floatingActionButton2 = (FloatingActionButton) o00O0000.OooooOo(view, R.id.fab_tips);
                                                                    if (floatingActionButton2 != null) {
                                                                        LinearLayoutCompat linearLayoutCompat2 = (LinearLayoutCompat) o00O0000.OooooOo(view, R.id.ll_bottom);
                                                                        if (linearLayoutCompat2 != null) {
                                                                            ProgressBar progressBar = (ProgressBar) o00O0000.OooooOo(view, R.id.pb_load);
                                                                            if (progressBar != null) {
                                                                                AppCompatImageButton appCompatImageButton2 = (AppCompatImageButton) o00O0000.OooooOo(view, R.id.tv_go);
                                                                                if (appCompatImageButton2 != null) {
                                                                                    WebView webView = (WebView) o00O0000.OooooOo(view, R.id.wv_course);
                                                                                    if (webView != null) {
                                                                                        this.f8237Ooooo0o = new o00O00OO.OooO0o((LinearLayoutCompat) view, linearLayoutCompat, appCompatImageButton, materialButton, chipGroup, chip, chip2, chip3, chip4, chip5, chip6, chip7, chip8, materialCardView, textInputEditText, floatingActionButton, floatingActionButton2, linearLayoutCompat2, progressBar, appCompatImageButton2, webView);
                                                                                        WebView.setWebContentsDebuggingEnabled(true);
                                                                                        o00O00OO.OooO0o oooO0o = this.f8237Ooooo0o;
                                                                                        if (oooO0o == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        LinearLayoutCompat linearLayoutCompat3 = oooO0o.f11078OooO0oo;
                                                                                        Context context = view.getContext();
                                                                                        kotlin.jvm.internal.OooOO0O.OooO0Oo(context, "getContext(...)");
                                                                                        com.suda.yzune.wakeupschedule.utils.o00Oo0 o00oo0 = new com.suda.yzune.wakeupschedule.utils.o00Oo0((int) (8 * context.getResources().getDisplayMetrics().density));
                                                                                        WeakHashMap weakHashMap = o00O00o0.f1458OooO00o;
                                                                                        Oooo0oO.o00O0000.OooOOO0(linearLayoutCompat3, o00oo0);
                                                                                        o00O00OO.OooO0o oooO0o2 = this.f8237Ooooo0o;
                                                                                        if (oooO0o2 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        Oooo0oO.o00O0000.OooOOO0(oooO0o2.f11089OooOo, new Oooo0oO.o000000O(17));
                                                                                        String str3 = this.f8236Ooooo00;
                                                                                        if (str3 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("url");
                                                                                            throw null;
                                                                                        }
                                                                                        if (str3.equals("")) {
                                                                                            SharedPreferences OooO0oo2 = com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0oo(Oooo0o0(), "config");
                                                                                            OooOo00 OoooOO02 = OoooOO0();
                                                                                            String str4 = OoooOO02.f8193OooO0o;
                                                                                            if (str4 == null) {
                                                                                                str4 = "";
                                                                                            }
                                                                                            String string = OooO0oo2.getString("school_url" + str4 + OoooOO02.f8196OooO0oo, "");
                                                                                            if (kotlin.jvm.internal.OooOO0O.OooO00o(string, "")) {
                                                                                                o00O00OO.OooO0o oooO0o3 = this.f8237Ooooo0o;
                                                                                                if (oooO0o3 == null) {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                                oooO0o3.f11096OooOoOO.setVisibility(0);
                                                                                                if (OoooOO0().f8196OooO0oo != 2) {
                                                                                                    o00O00OO.OooO0o oooO0o4 = this.f8237Ooooo0o;
                                                                                                    if (oooO0o4 == null) {
                                                                                                        kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                        throw null;
                                                                                                    }
                                                                                                    String str5 = OoooOO0().f8193OooO0o;
                                                                                                    if (str5 == null) {
                                                                                                        str5 = "";
                                                                                                    }
                                                                                                    oooO0o4.f11096OooOoOO.loadUrl("https://metaso.cn/?s=wakeup&referrer_s=wakeup&q=" + str5 + (OoooOO0().f8196OooO0oo == 1 ? "研究生" : "") + "教务系统网址");
                                                                                                } else {
                                                                                                    o00O00OO.OooO0o oooO0o5 = this.f8237Ooooo0o;
                                                                                                    if (oooO0o5 == null) {
                                                                                                        kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                        throw null;
                                                                                                    }
                                                                                                    oooO0o5.f11096OooOoOO.loadUrl("file:///android_asset/empty.html");
                                                                                                }
                                                                                            } else {
                                                                                                o00O00OO.OooO0o oooO0o6 = this.f8237Ooooo0o;
                                                                                                if (oooO0o6 == null) {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                                oooO0o6.f11091OooOo00.setVisibility(0);
                                                                                                o00O00OO.OooO0o oooO0o7 = this.f8237Ooooo0o;
                                                                                                if (oooO0o7 == null) {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                                final int i5 = 3;
                                                                                                oooO0o7.f11079OooOO0.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                                    /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                                    public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                                    {
                                                                                                        this.f8268OooO0oo = this;
                                                                                                    }

                                                                                                    /* JADX WARN: Removed duplicated region for block: B:94:0x0192  */
                                                                                                    /* JADX WARN: Removed duplicated region for block: B:96:0x01a5  */
                                                                                                    /* JADX WARN: Type inference failed for: r1v48, types: [T, kotlin.text.Regex] */
                                                                                                    @Override // android.view.View.OnClickListener
                                                                                                    /*
                                                                                                        Code decompiled incorrectly, please refer to instructions dump.
                                                                                                        To view partially-correct code enable 'Show inconsistent code' option in preferences
                                                                                                    */
                                                                                                    public final void onClick(android.view.View r13) {
                                                                                                        /*
                                                                                                            Method dump skipped, instructions count: 1054
                                                                                                            To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                        */
                                                                                                        throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                                    }
                                                                                                });
                                                                                                o00O00OO.OooO0o oooO0o8 = this.f8237Ooooo0o;
                                                                                                if (oooO0o8 == null) {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                                oooO0o8.f11090OooOo0.setText(string);
                                                                                                o00O00OO.OooO0o oooO0o9 = this.f8237Ooooo0o;
                                                                                                if (oooO0o9 == null) {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                                kotlin.jvm.internal.OooOO0O.OooO0O0(string);
                                                                                                oooO0o9.f11090OooOo0.setSelection(string.length());
                                                                                                o000oOoO();
                                                                                            }
                                                                                        } else {
                                                                                            o00O00OO.OooO0o oooO0o10 = this.f8237Ooooo0o;
                                                                                            if (oooO0o10 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            String str6 = this.f8236Ooooo00;
                                                                                            if (str6 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("url");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o10.f11090OooOo0.setText(str6);
                                                                                            o00O00OO.OooO0o oooO0o11 = this.f8237Ooooo0o;
                                                                                            if (oooO0o11 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            String str7 = this.f8236Ooooo00;
                                                                                            if (str7 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("url");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o11.f11090OooOo0.setSelection(str7.length());
                                                                                            o000oOoO();
                                                                                        }
                                                                                        if (!kotlin.jvm.internal.OooOO0O.OooO00o(OoooOO0().f8193OooO0o, "强智教务") && ((str2 = OoooOO0().f8195OooO0oO) == null || !kotlin.text.o0O0O00.o00Ooo(str2, "qz", false))) {
                                                                                            o00O00OO.OooO0o oooO0o12 = this.f8237Ooooo0o;
                                                                                            if (oooO0o12 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o12.f11080OooOO0O.setVisibility(8);
                                                                                        } else if (o00OO00o.o000OOo.OoooO00(new String[]{"qz", "qz_2017", "qz_with_node", "qz_crazy", "qz_br", "qz_2024"}, OoooOO0().f8195OooO0oO)) {
                                                                                            o00O00OO.OooO0o oooO0o13 = this.f8237Ooooo0o;
                                                                                            if (oooO0o13 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o13.f11080OooOO0O.setVisibility(0);
                                                                                            String str8 = OoooOO0().f8195OooO0oO;
                                                                                            if (str8 != null) {
                                                                                                switch (str8.hashCode()) {
                                                                                                    case -1209937157:
                                                                                                        if (str8.equals("qz_crazy")) {
                                                                                                            o00O00OO.OooO0o oooO0o14 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o14 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o14.f11087OooOOo0.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                    case 3625:
                                                                                                        if (str8.equals("qz")) {
                                                                                                            o00O00OO.OooO0o oooO0o15 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o15 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o15.f11082OooOOO.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                    case 108086822:
                                                                                                        if (str8.equals("qz_br")) {
                                                                                                            o00O00OO.OooO0o oooO0o16 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o16 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o16.f11084OooOOOO.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                    case 790729018:
                                                                                                        if (str8.equals("qz_2017")) {
                                                                                                            o00O00OO.OooO0o oooO0o17 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o17 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o17.f11086OooOOo.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                    case 790729046:
                                                                                                        if (str8.equals("qz_2024")) {
                                                                                                            o00O00OO.OooO0o oooO0o18 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o18 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o18.f11088OooOOoo.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                    case 940337989:
                                                                                                        if (str8.equals("qz_with_node")) {
                                                                                                            o00O00OO.OooO0o oooO0o19 = this.f8237Ooooo0o;
                                                                                                            if (oooO0o19 == null) {
                                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                                throw null;
                                                                                                            }
                                                                                                            oooO0o19.f11085OooOOOo.setChecked(true);
                                                                                                            break;
                                                                                                        }
                                                                                                        break;
                                                                                                }
                                                                                            }
                                                                                        } else {
                                                                                            o00O00OO.OooO0o oooO0o20 = this.f8237Ooooo0o;
                                                                                            if (oooO0o20 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o20.f11080OooOO0O.setVisibility(8);
                                                                                        }
                                                                                        o00O0O0.OooO0OO oooO0OO = new o00O0O0.OooO0OO(Oooo0o0());
                                                                                        androidx.appcompat.app.OooO oooO = (androidx.appcompat.app.OooO) oooO0OO.f881OooO;
                                                                                        oooO.OooO0Oo = "注意事项";
                                                                                        oooO.f1941OooO0o = (SpannableStringBuilder) this.f8245o00O0O.getValue();
                                                                                        oooO0OO.OooOOOO("我知道啦", null);
                                                                                        oooO0OO.OooOO0o("如何正确选择教务？", new o000000(this, i3));
                                                                                        oooO.f1949OooOOO0 = false;
                                                                                        this.f8238OooooO0 = oooO0OO.OooO0o();
                                                                                        o00O00OO.OooO0o oooO0o21 = this.f8237Ooooo0o;
                                                                                        if (oooO0o21 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o21.f11096OooOoOO.getSettings().setJavaScriptEnabled(true);
                                                                                        o00O00OO.OooO0o oooO0o22 = this.f8237Ooooo0o;
                                                                                        if (oooO0o22 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o22.f11096OooOoOO.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                                                                                        o00O00OO.OooO0o oooO0o23 = this.f8237Ooooo0o;
                                                                                        if (oooO0o23 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o23.f11096OooOoOO.getSettings().setMixedContentMode(0);
                                                                                        o00O00OO.OooO0o oooO0o24 = this.f8237Ooooo0o;
                                                                                        if (oooO0o24 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o24.f11096OooOoOO.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
                                                                                        o00O00OO.OooO0o oooO0o25 = this.f8237Ooooo0o;
                                                                                        if (oooO0o25 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o25.f11096OooOoOO.setWebViewClient(new o00000OO(this));
                                                                                        o00O0O0.OooO0OO oooO0OO2 = new o00O0O0.OooO0OO(Oooo0o0());
                                                                                        oooO0OO2.OooOOo0(R.string.title_tips);
                                                                                        ((androidx.appcompat.app.OooO) oooO0OO2.f881OooO).f1941OooO0o = "当前页面是否显示完整？如果不完整，可以尝试反复点击底栏的「电脑」按钮刷新页面。";
                                                                                        oooO0OO2.OooOOO(R.string.ok, null);
                                                                                        oooO0OO2.OooOO0o("不再提醒", new o000000(this, i2));
                                                                                        o00O00OO.OooO0o oooO0o26 = this.f8237Ooooo0o;
                                                                                        if (oooO0o26 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o26.f11096OooOoOO.setWebChromeClient(new o0000(this, oooO0OO2));
                                                                                        o00O00OO.OooO0o oooO0o27 = this.f8237Ooooo0o;
                                                                                        if (oooO0o27 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o27.f11096OooOoOO.getSettings().setUseWideViewPort(true);
                                                                                        o00O00OO.OooO0o oooO0o28 = this.f8237Ooooo0o;
                                                                                        if (oooO0o28 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o28.f11096OooOoOO.getSettings().setSupportZoom(true);
                                                                                        o00O00OO.OooO0o oooO0o29 = this.f8237Ooooo0o;
                                                                                        if (oooO0o29 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o29.f11096OooOoOO.getSettings().setBuiltInZoomControls(true);
                                                                                        o00O00OO.OooO0o oooO0o30 = this.f8237Ooooo0o;
                                                                                        if (oooO0o30 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o30.f11096OooOoOO.getSettings().setDisplayZoomControls(false);
                                                                                        o00O00OO.OooO0o oooO0o31 = this.f8237Ooooo0o;
                                                                                        if (oooO0o31 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o31.f11096OooOoOO.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                                                                                        o00O00OO.OooO0o oooO0o32 = this.f8237Ooooo0o;
                                                                                        if (oooO0o32 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o32.f11096OooOoOO.getSettings().setDomStorageEnabled(true);
                                                                                        CookieManager cookieManager = CookieManager.getInstance();
                                                                                        o00O00OO.OooO0o oooO0o33 = this.f8237Ooooo0o;
                                                                                        if (oooO0o33 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        cookieManager.setAcceptThirdPartyCookies(oooO0o33.f11096OooOoOO, true);
                                                                                        o00O00OO.OooO0o oooO0o34 = this.f8237Ooooo0o;
                                                                                        if (oooO0o34 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        final int i6 = 4;
                                                                                        oooO0o34.f11093OooOo0o.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                            /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                            public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                            {
                                                                                                this.f8268OooO0oo = this;
                                                                                            }

                                                                                            @Override // android.view.View.OnClickListener
                                                                                            public final void onClick(View view2) {
                                                                                                /*
                                                                                                    Method dump skipped, instructions count: 1054
                                                                                                    To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                */
                                                                                                throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                            }
                                                                                        });
                                                                                        o00O00OO.OooO0o oooO0o35 = this.f8237Ooooo0o;
                                                                                        if (oooO0o35 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o35.f11081OooOO0o.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.suda.yzune.wakeupschedule.schedule_import.o00000O
                                                                                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                                                                                            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                                                                                WebViewLoginFragment webViewLoginFragment = WebViewLoginFragment.this;
                                                                                                webViewLoginFragment.f8244Ooooooo = z;
                                                                                                SharedPreferences.Editor edit = com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0oo(webViewLoginFragment.Oooo0o0(), "config").edit();
                                                                                                edit.putBoolean("is_webview_desktop_mode", z);
                                                                                                edit.apply();
                                                                                                if (z) {
                                                                                                    o00O00OO.OooO0o oooO0o36 = webViewLoginFragment.f8237Ooooo0o;
                                                                                                    if (oooO0o36 == null) {
                                                                                                        kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                        throw null;
                                                                                                    }
                                                                                                    oooO0o36.f11096OooOoOO.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 Safari/537.36");
                                                                                                } else {
                                                                                                    o00O00OO.OooO0o oooO0o37 = webViewLoginFragment.f8237Ooooo0o;
                                                                                                    if (oooO0o37 == null) {
                                                                                                        kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                        throw null;
                                                                                                    }
                                                                                                    oooO0o37.f11096OooOoOO.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13; PGT-AN10; HMSCore 6.11.0.332) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 HuaweiBrowser/14.0.2.300 Mobile Safari/537.36");
                                                                                                }
                                                                                                o00O00OO.OooO0o oooO0o38 = webViewLoginFragment.f8237Ooooo0o;
                                                                                                if (oooO0o38 != null) {
                                                                                                    oooO0o38.f11096OooOoOO.reload();
                                                                                                } else {
                                                                                                    kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                    throw null;
                                                                                                }
                                                                                            }
                                                                                        });
                                                                                        if (kotlin.jvm.internal.OooOO0O.OooO00o(OoooOO0().f8195OooO0oO, "ztvtit") || kotlin.jvm.internal.OooOO0O.OooO00o(OoooOO0().f8193OooO0o, "西安科技大学") || kotlin.jvm.internal.OooOO0O.OooO00o(OoooOO0().f8193OooO0o, "江苏师范大学") || ((str = OoooOO0().f8193OooO0o) != null && kotlin.text.o00Ooo.o00o0O(str, "浙江万里学院", false))) {
                                                                                            o00O00OO.OooO0o oooO0o36 = this.f8237Ooooo0o;
                                                                                            if (oooO0o36 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o36.f11081OooOO0o.setChecked(true);
                                                                                        } else {
                                                                                            o00O00OO.OooO0o oooO0o37 = this.f8237Ooooo0o;
                                                                                            if (oooO0o37 == null) {
                                                                                                kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                                throw null;
                                                                                            }
                                                                                            oooO0o37.f11081OooOO0o.setChecked(com.suda.yzune.wakeupschedule.utils.OooO0o.OooO0oo(Oooo0o0(), "config").getBoolean("is_webview_desktop_mode", false));
                                                                                        }
                                                                                        o00O00OO.OooO0o oooO0o38 = this.f8237Ooooo0o;
                                                                                        if (oooO0o38 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        final int i7 = 5;
                                                                                        oooO0o38.f11083OooOOO0.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                            /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                            public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                            {
                                                                                                this.f8268OooO0oo = this;
                                                                                            }

                                                                                            @Override // android.view.View.OnClickListener
                                                                                            public final void onClick(View view2) {
                                                                                                /*
                                                                                                    Method dump skipped, instructions count: 1054
                                                                                                    To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                */
                                                                                                throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                            }
                                                                                        });
                                                                                        Ref$IntRef ref$IntRef = new Ref$IntRef();
                                                                                        ref$IntRef.element = R.id.chip_qz1;
                                                                                        o00O00OO.OooO0o oooO0o39 = this.f8237Ooooo0o;
                                                                                        if (oooO0o39 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o39.f11080OooOO0O.setOnCheckedChangeListener(new androidx.fragment.app.OooO0o(this, 6, ref$IntRef));
                                                                                        o00O00OO.OooO0o oooO0o40 = this.f8237Ooooo0o;
                                                                                        if (oooO0o40 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o40.f11094OooOoO.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                            /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                            public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                            {
                                                                                                this.f8268OooO0oo = this;
                                                                                            }

                                                                                            @Override // android.view.View.OnClickListener
                                                                                            public final void onClick(View view2) {
                                                                                                /*
                                                                                                    Method dump skipped, instructions count: 1054
                                                                                                    To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                */
                                                                                                throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                            }
                                                                                        });
                                                                                        o00O00OO.OooO0o oooO0o41 = this.f8237Ooooo0o;
                                                                                        if (oooO0o41 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o41.f11090OooOo0.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.suda.yzune.wakeupschedule.schedule_import.o0O0O00
                                                                                            @Override // android.widget.TextView.OnEditorActionListener
                                                                                            public final boolean onEditorAction(TextView textView, int i8, KeyEvent keyEvent) {
                                                                                                if (i8 != 6) {
                                                                                                    return false;
                                                                                                }
                                                                                                WebViewLoginFragment.this.o000oOoO();
                                                                                                return false;
                                                                                            }
                                                                                        });
                                                                                        o00O00OO.OooO0o oooO0o42 = this.f8237Ooooo0o;
                                                                                        if (oooO0o42 == null) {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                        oooO0o42.f11092OooOo0O.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                            /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                            public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                            {
                                                                                                this.f8268OooO0oo = this;
                                                                                            }

                                                                                            @Override // android.view.View.OnClickListener
                                                                                            public final void onClick(View view2) {
                                                                                                /*
                                                                                                    Method dump skipped, instructions count: 1054
                                                                                                    To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                */
                                                                                                throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                            }
                                                                                        });
                                                                                        o00O00OO.OooO0o oooO0o43 = this.f8237Ooooo0o;
                                                                                        if (oooO0o43 != null) {
                                                                                            oooO0o43.f11076OooO.setOnClickListener(new View.OnClickListener(this) { // from class: com.suda.yzune.wakeupschedule.schedule_import.o000OOo

                                                                                                /* renamed from: OooO0oo, reason: collision with root package name */
                                                                                                public final /* synthetic */ WebViewLoginFragment f8268OooO0oo;

                                                                                                {
                                                                                                    this.f8268OooO0oo = this;
                                                                                                }

                                                                                                @Override // android.view.View.OnClickListener
                                                                                                public final void onClick(View view2) {
                                                                                                    /*
                                                                                                        Method dump skipped, instructions count: 1054
                                                                                                        To view this dump change 'Code comments level' option to 'DEBUG'
                                                                                                    */
                                                                                                    throw new UnsupportedOperationException("Method not decompiled: com.suda.yzune.wakeupschedule.schedule_import.o000OOo.onClick(android.view.View):void");
                                                                                                }
                                                                                            });
                                                                                            return;
                                                                                        } else {
                                                                                            kotlin.jvm.internal.OooOO0O.OooOO0o("binding");
                                                                                            throw null;
                                                                                        }
                                                                                    }
                                                                                    i4 = R.id.wv_course;
                                                                                } else {
                                                                                    i4 = R.id.tv_go;
                                                                                }
                                                                            } else {
                                                                                i4 = R.id.pb_load;
                                                                            }
                                                                        } else {
                                                                            i4 = R.id.ll_bottom;
                                                                        }
                                                                    } else {
                                                                        i4 = R.id.fab_tips;
                                                                    }
                                                                } else {
                                                                    i4 = R.id.fab_import;
                                                                }
                                                            } else {
                                                                i4 = R.id.et_url;
                                                            }
                                                        } else {
                                                            i4 = R.id.cv_url_tips;
                                                        }
                                                    } else {
                                                        i4 = R.id.chip_qz6;
                                                    }
                                                } else {
                                                    i4 = R.id.chip_qz5;
                                                }
                                            } else {
                                                i4 = R.id.chip_qz4;
                                            }
                                        } else {
                                            i4 = R.id.chip_qz3;
                                        }
                                    } else {
                                        i4 = R.id.chip_qz2;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i4)));
    }
} new {
    
}
