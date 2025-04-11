/**
 * @Author: 雨中人
 * @：人工智能222万世杰
 * @Date: 2024-04-11 
 * @LastEditTime: 2024-07-7 21:57:50
 * @LastEditors: xiaoxiao
 * @Description: 苦心编写多日，参考了大佬模板
 * @QQ：2190638246
 * https://github.com/wanshijie1
 */
function SHA1(s) {
    function encodeUTF8(s) {
        var i, r = [],
            c, x;
        for (i = 0; i < s.length; i++)
            if ((c = s.charCodeAt(i)) < 0x80) r.push(c);
            else if (c < 0x800) r.push(0xC0 + (c >> 6 & 0x1F), 0x80 + (c & 0x3F));
        else {
            if ((x = c ^ 0xD800) >> 10 == 0) //对四字节UTF-16转换为Unicode
                c = (x << 10) + (s.charCodeAt(++i) ^ 0xDC00) + 0x10000,
                r.push(0xF0 + (c >> 18 & 0x7), 0x80 + (c >> 12 & 0x3F));
            else r.push(0xE0 + (c >> 12 & 0xF));
            r.push(0x80 + (c >> 6 & 0x3F), 0x80 + (c & 0x3F));
        };
        return r;
    }
    var data = new Uint8Array(encodeUTF8(s))
    var i, j, t;
    var l = ((data.length + 8) >>> 6 << 4) + 16,
        s = new Uint8Array(l << 2);
    s.set(new Uint8Array(data.buffer)), s = new Uint32Array(s.buffer);
    for (t = new DataView(s.buffer), i = 0; i < l; i++) s[i] = t.getUint32(i << 2);
    s[data.length >> 2] |= 0x80 << (24 - (data.length & 3) * 8);
    s[l - 1] = data.length << 3;
    var w = [],
        f = [
            function() { return m[1] & m[2] | ~m[1] & m[3]; },
            function() { return m[1] ^ m[2] ^ m[3]; },
            function() { return m[1] & m[2] | m[1] & m[3] | m[2] & m[3]; },
            function() { return m[1] ^ m[2] ^ m[3]; }
        ],
        rol = function(n, c) { return n << c | n >>> (32 - c); },
        k = [1518500249, 1859775393, -1894007588, -899497514],
        m = [1732584193, -271733879, null, null, -1009589776];
    m[2] = ~m[0], m[3] = ~m[1];
    for (i = 0; i < s.length; i += 16) {
        var o = m.slice(0);
        for (j = 0; j < 80; j++)
            w[j] = j < 16 ? s[i + j] : rol(w[j - 3] ^ w[j - 8] ^ w[j - 14] ^ w[j - 16], 1),
            t = rol(m[0], 5) + f[j / 20 | 0]() + m[4] + w[j] + k[j / 20 | 0] | 0,
            m[1] = rol(m[1], 30), m.pop(), m.unshift(t);
        for (j = 0; j < 5; j++) m[j] = m[j] + o[j] | 0;
    };
    t = new DataView(new Uint32Array(m).buffer);
    for (var i = 0; i < 5; i++) m[i] = t.getUint32(i << 2);

    var hex = Array.prototype.map.call(new Uint8Array(new Uint32Array(m).buffer), function(e) {
        return (e < 16 ? "0" : "") + e.toString(16);
    }).join("");
    return hex;
}
let textToDom = (text) => {
        let parser = new DOMParser()
        return parser.parseFromString(text, "text/html")
    }
    //添加img元素
let addImg = (url) => {
    let addInterval = setInterval(addFun, "100")

    function addFun() {
        let aiDiv = document.getElementsByTagName("ai-schedule-div")
        if (aiDiv.length != 0) {
            let img = document.createElement("img")
            img.src = url;
            img.style.cssText = "display: block; width: 50%; max-width: 200px; min-height: 11vw; max-height: 6vh; position: relative; overflow: auto; margin-top:0vh; padding: 2vw;"
            img.setAttribute("onclick", "this.src='" + url + "'")
            aiDiv[2].appendChild(img)
            clearInterval(addInterval)
        }
    }

}
async function getSjarr(sha, dom, prul, urls) {

    let username = document.getElementById("username").value
    let pas = document.getElementById("password").value

    username = !username ? await AISchedulePrompt({
        titleText: "请输入用户名",
        tipText: "",
        defaultText: "",
        validator: (username) => {
            if (!username) return "用户名输入有误";
            else return false
        }
    }) : username;
    pas = !pas ? await AISchedulePrompt({
        titleText: "请输入密码",
        tipText: "",
        defaultText: "",
        validator: (password) => {
            if (!password) return "密码输入有误";
            else return false
        }
    }) : pas;
    pas = SHA1(sha + pas);
    //  pas = new Base64().encode(sha+pas)
    let loginData = new FormData()
    loginData.set("username", username)
    loginData.set("password", pas)
    loginData.set("pwd", pas)
    loginData.set("session_locale", "zh_CN")

    let vim = dom.getElementsByClassName("verity-image")
    let cr = dom.getElementsByClassName("captcha_response")
    if (vim.length != 0 || cr.length != 0) {
        addImg(!vim.length ? cr.nextElementSibling.src : vim[0].childNodes[0].src)
        loginData.set("encodedPassword=", "")
        loginData.set("captcha_response",
            await AISchedulePrompt({
                titleText: "请输入页面验证码",
                tipText: "",
                defaultText: "",
                validator: (yzm) => {
                    if (!yzm) return "验证码输入有误";
                    else return false
                }
            })
        )

    }
    if (username == null || username.length == 0) {
        return false;
    } else {

        let logRe = await request("POST", loginData, prul + urls.login);
        console.log(logRe)
        let tdom = textToDom(logRe);
        let errtext = tdom.getElementsByClassName("actionError")
        if (!!errtext.length) {
            await AIScheduleAlert({
                contentText: errtext[0].innerText + ">>>请退出重新进入<<<",
                titleText: '错误',
                confirmText: '确认',
            })
            return ""
        }
        console.info("登录中。。。")
        return getSjarr1(prul);
    }

}

async function request(method, data, url) {
    return await fetch(url, { method: method, body: data }).then(v => v.text()).then(v => v).catch(v => v)
}

function sleep(timeout) {
    for (let t = Date.now(); Date.now() - t <= timeout * 1000;);
}

async function getSemestersId(preUrl, courseTableCon) {
    let semesterIds = []
    let mess = "";
    // 修正API URL路径，确保在WebVPN环境下正确
    let xqurl = "";

    // 针对WebVPN环境的特殊处理
    if (preUrl.includes("webvpn")) {
        // 尝试多种可能的URL路径
        if (preUrl.endsWith("/")) {
            xqurl = preUrl + "dataQuery.action";
        } else if (preUrl.endsWith("/ahpu")) {
            xqurl = preUrl + "/dataQuery.action";
        } else {
            xqurl = preUrl + "/ahpu/dataQuery.action";
        }
        console.log("WebVPN环境下的查询URL:", xqurl);
    } else {
        xqurl = preUrl + "/dataQuery.action";
    }

    Android.onProgress("查询URL: " + xqurl);

    try {
        // 首先尝试使用正则表达式提取需要的参数
        let tagId = "";
        let value = "";

        try {
            // 尝试从courseTableCon中提取tagId
            let semesterBarMatch = courseTableCon.match(/semesterBar(.*?)Semester/);
            if (semesterBarMatch) {
                tagId = "semesterBar" + semesterBarMatch[1] + "Semester";
            } else {
                tagId = "semesterBar13579Semester"; // 使用一个通用的fallback值
            }

            // 尝试从courseTableCon中提取value
            let valueMatch = courseTableCon.match(/value:"(.*?)"/);
            if (valueMatch) {
                value = valueMatch[1];
            } else {
                value = "194"; // 使用一个通用的fallback值
            }

            console.log("提取参数 - tagId:", tagId, "value:", value);
        } catch (e) {
            console.error("正则提取参数失败:", e);
            // 使用默认值
            tagId = "semesterBar13579Semester";
            value = "194";
        }

        let xqdata = new FormData()
        xqdata.set("tagId", tagId)
        xqdata.set("dataType", "semesterCalendar")
        xqdata.set("value", value)
        xqdata.set("empty", false)

        Android.onProgress("正在获取学期数据...");

        // 获取原始响应
        let rawResponse = await request("post", xqdata, xqurl);
        console.log("原始学期数据响应:", rawResponse);

        // 检查响应是否为空或格式错误
        if (!rawResponse || rawResponse.length < 10) {
            throw new Error("学期数据响应为空或太短");
        }

        // 尝试直接解析JSON
        let jsonData;
        try {
            jsonData = JSON.parse(rawResponse);
        } catch (e) {
            console.error("JSON解析失败，尝试清理响应数据:", e);

            // 尝试清理响应数据
            let cleanResponse = rawResponse
                .replace(/^[^{]*/, '') // 删除开头直到第一个{
                .replace(/[^}]*$/, ''); // 删除最后一个}之后的内容

            try {
                jsonData = JSON.parse(cleanResponse);
            } catch (e2) {
                console.error("清理后仍然无法解析:", e2);
                throw new Error("无法解析学期数据");
            }
        }

        console.log("解析后的学期数据:", JSON.stringify(jsonData));

        // 检查是否有semesters属性
        if (!jsonData.semesters) {
            throw new Error("学期数据格式不正确，缺少semesters属性");
        }

        let semesters = jsonData.semesters;
        let currentYear = new Date().getFullYear();
        let selectList = [];
        let index = 0;

        // 遍历学期数据
        for (let key in semesters) {
            if (Array.isArray(semesters[key]) && semesters[key].length > 0) {
                for (let item of semesters[key]) {
                    // 尝试提取学年和学期名称
                    let schoolYear = item.schoolYear || "";
                    let name = item.name || "";
                    let id = item.id || "";

                    if (id) {
                        let option = index + ":" + schoolYear + '学年' + name + "学期";
                        console.log("添加学期选项:", option, "ID:", id);

                        selectList.push(option);
                        semesterIds.push(id);
                        index++;
                    }
                }
            }
        }

        // 如果没有找到学期，添加默认选项
        if (selectList.length === 0) {
            console.log("未找到有效学期数据，使用默认选项");
            // 默认添加3个最常见的学期
            selectList = [
                "0:2023-2024学年第二学期",
                "1:2023-2024学年第一学期",
                "2:2022-2023学年第二学期"
            ];

            // 使用通用的学期ID
            semesterIds = ["62", "61", "60"];
        }

        // 记录最终选项列表
        console.log("最终学期选项(", selectList.length, "):", selectList.join(", "));
        Android.onProgress("已找到 " + selectList.length + " 个学期");

        // 弹出选择对话框
        let result = await AIScheduleSelect({
            titleText: "选择学期",
            contentText: "请选择需要导入的学期",
            selectList: selectList
        });

        console.log("选择结果:", result);

        // 解析选择结果
        let semesterIndex = 0;
        try {
            semesterIndex = parseInt(result.split(":")[0]);
            if (isNaN(semesterIndex)) semesterIndex = 0;
        } catch (e) {
            console.error("解析选择结果失败:", e);
            semesterIndex = 0;
        }

        return {
            'semesterIds': semesterIds,
            'semesterIndex': semesterIndex
        };
    } catch (error) {
        console.error("获取学期数据出错:", error);
        Android.onProgress("获取学期数据出错: " + (error.message || "未知错误") + "，将使用默认选项");

        // 错误时提供默认选项
        let selectList = [
            "0:2023-2024学年第二学期",
            "1:2023-2024学年第一学期",
            "2:2022-2023学年第二学期"
        ];
        let defaultIds = ["62", "61", "60"];

        let result = await AIScheduleSelect({
            titleText: "学期(数据获取出错)",
            contentText: "请选择学期，将尝试使用这些默认ID",
            selectList: selectList
        });

        let semesterIndex = 0;
        try {
            semesterIndex = parseInt(result.split(":")[0]);
            if (isNaN(semesterIndex)) semesterIndex = 0;
        } catch (e) {
            semesterIndex = 0;
        }

        return {
            'semesterIds': defaultIds,
            'semesterIndex': semesterIndex
        };
    }
}


async function getSjarr1(preUrl) {
    try {
        sleep(0.35)
        Android.onProgress("正在获取课表页面...");

        // 修正课表URL路径
        let idurl = "";
        if (preUrl.includes("webvpn")) {
            // 尝试多种可能的URL路径
            if (preUrl.endsWith("/")) {
                idurl = preUrl + "courseTableForStd.action";
            } else if (preUrl.endsWith("/ahpu")) {
                idurl = preUrl + "/courseTableForStd.action";
            } else {
                idurl = preUrl + "/ahpu/courseTableForStd.action";
            }
            console.log("WebVPN环境下的课表URL:", idurl);
        } else {
            idurl = preUrl + "/courseTableForStd.action";
        }

        Android.onProgress("课表页面URL: " + idurl);
        let courseTableCon = await request("get", null, idurl);

        if (!courseTableCon || courseTableCon.length < 100) {
            throw new Error("获取课表页面失败或内容不完整");
        }

        console.info("获取学期信息...");
        Android.onProgress("正在获取学期信息...");

        let semIdsJson = await getSemestersId(preUrl, courseTableCon);
        console.log("学期信息:", JSON.stringify(semIdsJson));

        // 尝试获取ids
        let ids = null;
        try {
            // 尝试使用不同的正则表达式匹配ids
            const patterns = [
                /bg\.form\.addInput\(form,"ids","([^"]*)"\);/,
                /name="ids" value="([^"]*)"/,
                /ids=([^&"]*)/
            ];

            for (const pattern of patterns) {
                const match = courseTableCon.match(pattern);
                if (match && match[1]) {
                    ids = match[1];
                    console.log("通过模式匹配到ids:", ids);
                    break;
                }
            }

            // 如果所有正则都失败，尝试寻找任何可能的ids
            if (!ids) {
                const anyIdsMatch = courseTableCon.match(/ids["']?\s*[:=]\s*["']?([^"',;\s]+)/);
                if (anyIdsMatch && anyIdsMatch[1]) {
                    ids = anyIdsMatch[1];
                    console.log("通过通用匹配找到ids:", ids);
                }
            }
        } catch (e) {
            console.error("匹配ids失败:", e);
        }

        // 如果还是没有匹配到ids，使用一个通用值
        if (!ids) {
            ids = "194";
            console.warn("未能找到ids，使用默认值:", ids);
            Android.onProgress("未能找到课表ID，将使用默认值尝试");
        } else {
            Android.onProgress("已获取课表ID: " + ids);
        }

        let courseArr = [];
        let i = parseInt(semIdsJson.semesterIndex) || 0;
        let attempts = 0;

        while (courseArr.length <= 1 && i >= 0 && attempts < 3) {
            try {
                sleep(0.4);
                const semesterId = semIdsJson.semesterIds[i] || "62"; // 使用默认学期ID作为后备

                console.info("正在查询课表, 学期ID:", semesterId);
                Android.onProgress("正在获取第 " + (attempts + 1) + " 次课表数据...");

                let formData = new FormData();
                formData.set("ignoreHead", 1);
                formData.set("setting.kind", "std");
                formData.set("startWeek", "");
                formData.set("semester.id", semesterId);
                formData.set("ids", ids);

                // 修正课表查询URL
                let courseQueryUrl = "";
                if (preUrl.includes("webvpn")) {
                    // 尝试多种可能的URL路径
                    if (preUrl.endsWith("/")) {
                        courseQueryUrl = preUrl + "courseTableForStd!courseTable.action";
                    } else if (preUrl.endsWith("/ahpu")) {
                        courseQueryUrl = preUrl + "/courseTableForStd!courseTable.action";
                    } else {
                        courseQueryUrl = preUrl + "/ahpu/courseTableForStd!courseTable.action";
                    }
                    console.log("WebVPN环境下的课表查询URL:", courseQueryUrl);
                } else {
                    courseQueryUrl = preUrl + "/courseTableForStd!courseTable.action";
                }

                // 使用修正后的URL
                Android.onProgress("请求课表: " + courseQueryUrl);
                let response = await request("post", formData, courseQueryUrl);

                // 检查响应是否包含课程数据
                if (response && response.includes("actTeacherName")) {
                    // 分割响应获取课程数据
                    courseArr = response.split(/var teachers = \[.*?\];/);
                    console.log("获取到课表数据，包含 " + courseArr.length + " 部分");

                    // 如果只有一部分（表示没有课程数据），尝试其他分割方法
                    if (courseArr.length <= 1) {
                        courseArr = response.split("var teachers =");
                        console.log("重新分割后有 " + courseArr.length + " 部分");
                    }
                } else {
                    console.warn("响应中未找到课程数据");
                }

                attempts++;
                i--;
            } catch (e) {
                console.error("获取课表数据失败:", e);
                attempts++;
                i--;
            }
        }

        // 如果所有尝试都失败，返回空数组
        if (courseArr.length <= 1) {
            console.warn("所有尝试都未能获取到课表数据");
            Android.onProgress("未能获取到课表数据，将返回空课表");
            return [];
        }

        Android.onProgress("成功获取课表数据，正在解析...");
        return courseArr;
    } catch (e) {
        console.error("获取课表失败:", e);
        Android.onProgress("获取课表失败: " + (e.message || "未知错误"));
        return [];
    }
}

function distinct(arr) {
    return Array.from(new Set(arr));
}

async function scheduleHtmlProvider(iframeContent = "", frameContent = "", dom = document) {
    try {
        await loadTool('AIScheduleTools');

        Android.onProgress("正在初始化导入流程...");

        let warning = `
            >>>导入流程<<<
        1. 请确保已登录教务系统
        2. 系统将自动获取学期和课表
        3. 若出错，会提供默认选项
            PS：因为新老校区上午上课时间不同，默认使用新校区教学时间
        `;

        await AIScheduleAlert(warning);

        // 提取当前URL信息
        let currentUrl = location.href;
        Android.onProgress("当前页面URL: " + currentUrl);

        let preUrl = "";
        let isWebVPN = false;

        // 检测是否在WebVPN环境
        if (currentUrl.includes("webvpn")) {
            isWebVPN = true;
            Android.onProgress("检测到WebVPN环境，将适配WebVPN URL");

            // 获取WebVPN前缀，通常格式为https://webvpn.xxx.edu.cn/http/xxx/
            let webvpnPrefix = currentUrl.match(/(https?:\/\/webvpn\.[^\/]+\/[^\/]+\/[^\/]+\/)/);
            if (webvpnPrefix && webvpnPrefix[1]) {
                preUrl = webvpnPrefix[1];
                console.log("WebVPN前缀: " + preUrl);
            } else {
                // 如果无法提取WebVPN前缀，使用备用方法
                let parts = currentUrl.split("/");
                // 找到最后出现的ahpu（安徽工程大学域名）的位置
                let ahpuIndex = -1;
                for (let i = parts.length - 1; i >= 0; i--) {
                    if (parts[i].includes("ahpu")) {
                        ahpuIndex = i;
                        break;
                    }
                }

                if (ahpuIndex > 0) {
                    preUrl = parts.slice(0, ahpuIndex + 1).join("/");
                }
            }
        } else {
            // 非WebVPN环境，直接获取基础URL
            let urlar = currentUrl.split("/");
            let verTag = urlar.pop();
            preUrl = urlar.join("/");
        }

        if (!preUrl) {
            // 如果无法获取preUrl，使用备用URL
            preUrl = "https://webvpn.ahpu.edu.cn/http/webvpnd0b10bdb350bb9543ce925f1f45f7a8d037396bf3b450ddbbfbddfc8f5414a4c/ahpu";
            console.warn("无法获取URL前缀，使用备用前缀:", preUrl);
        }

        console.log("Base URL: " + preUrl);
        Android.onProgress("已确定基础URL: " + preUrl);

        // 确定API URLs
        let loginPath = "/loginExt.action";
        let homePath = "/homeExt.action";
        let loginTableClassName = "login-table";

        // 针对WebVPN环境的特殊处理
        if (isWebVPN) {
            // 生成两种可能的URL，并测试它们
            let testUrls = [];

            // 方式1: 直接拼接
            if (preUrl.endsWith("/")) {
                testUrls.push(preUrl + "homeExt.action");
            } else {
                testUrls.push(preUrl + "/homeExt.action");
            }

            // 方式2: 包含ahpu路径
            if (!preUrl.endsWith("ahpu") && !preUrl.endsWith("ahpu/")) {
                if (preUrl.endsWith("/")) {
                    testUrls.push(preUrl + "ahpu/homeExt.action");
                } else {
                    testUrls.push(preUrl + "/ahpu/homeExt.action");
                }
            }

            // 直接测试最有可能成功的URL
            Android.onProgress("测试WebVPN环境下可能的URL...");
            let validUrl = null;

            for (let testUrl of testUrls) {
                try {
                    console.log("测试URL: " + testUrl);
                    let response = await request("get", null, testUrl);

                    // 如果响应中包含这些字符串，说明URL可能有效
                    if (response && (response.includes("<title>") ||
                            response.includes("loginForm") ||
                            response.includes("login") ||
                            !response.includes("404"))) {
                        console.log("URL测试成功: " + testUrl);
                        validUrl = testUrl;
                        break;
                    }
                } catch (e) {
                    console.error("测试URL失败: " + testUrl, e);
                }
            }

            // 使用测试成功的URL或者原始URL
            if (validUrl) {
                // 从有效URL中提取正确的前缀
                preUrl = validUrl.substring(0, validUrl.lastIndexOf("/"));
                Android.onProgress("找到有效的URL前缀: " + preUrl);
            }
        }

        // 检查URL是否包含Ext，如果不包含则使用非Ext版本的API
        if (!preUrl.includes("Ext")) {
            loginPath = "/login.action";
            homePath = "/home.action";
            loginTableClassName = "logintable";
        }

        // 自动检测WebVPN路径
        if (isWebVPN) {
            if (preUrl.endsWith("/")) {
                homePath = homePath.substring(1); // 移除前导斜杠
                loginPath = loginPath.substring(1);
            } else if (preUrl.endsWith("/ahpu")) {
                // 保持原样
            } else if (!preUrl.includes("/ahpu/")) {
                homePath = "/ahpu" + homePath;
                loginPath = "/ahpu" + loginPath;
            }
        }

        let urls = {
            "home": homePath,
            "login": loginPath,
            "loginTableClassName": loginTableClassName
        };

        console.log("最终home URL: " + preUrl + homePath);
        console.log("最终login URL: " + preUrl + loginPath);
        Android.onProgress("已确定API路径: " + preUrl + homePath);

        Android.onProgress("正在检查登录状态...");

        // 尝试请求首页检查登录状态
        try {
            // 检查是否已登录
            let homeText = await request("get", null, preUrl + urls.home);
            let homeDom = textToDom(homeText);
            let logintag = homeDom.getElementsByClassName(urls.loginTableClassName).length;

            if (currentUrl.includes("cas/login")) {
                Android.onProgress("检测到CAS登录页面，请先完成登录");
                return "do not continue";
            } else if (!logintag || currentUrl !== preUrl + urls.login) {
                // 已登录，直接获取课表
                Android.onProgress("检测到已登录状态，开始获取课表");
                let courseArr = await getSjarr1(preUrl);
                return processCourseData(courseArr);
            } else {
                // 未登录，尝试提取SHA1并登录
                Android.onProgress("检测到未登录状态，尝试提取登录参数");
                let sha = "";

                try {
                    // 尝试多种模式匹配SHA1
                    const patterns = [
                        /CryptoJS\.SHA1\(['"]([^'"]+)['"]/,
                        /b\.encode\(['"]([^'"]+)['"]/,
                        /SHA1\(['"]([^'"]+)['"]/
                    ];

                    for (const pattern of patterns) {
                        const match = homeText.match(pattern);
                        if (match && match[1]) {
                            sha = match[1];
                            break;
                        }
                    }

                    if (!sha) {
                        Android.onProgress("无法提取SHA1参数，请手动登录后再尝试");
                        return "do not continue";
                    }
                } catch (e) {
                    console.error("提取SHA1失败:", e);
                    Android.onProgress("提取登录参数失败，请手动登录");
                    return "do not continue";
                }

                // 使用提取的SHA1尝试登录
                let arr = await getSjarr(sha, homeDom, preUrl, urls);
                return processCourseData(arr);
            }
        } catch (e) {
            console.error("检查登录状态失败:", e);
            Android.onProgress("检查登录状态失败，请确保已登录教务系统");
            return "do not continue";
        }
    } catch (e) {
        console.error("scheduleHtmlProvider出错:", e);
        Android.onProgress("导入过程出错: " + e.message);
        return "do not continue";
    }
}

// 处理课程数据
function processCourseData(arr) {
    let courseArr = [];
    let message = "";

    try {
        if (arr && arr.length > 1) {
            arr.slice(1).forEach(courseText => {
                if (!courseText || courseText.trim() === "") return;

                try {
                    let course = { weeks: [], sections: [] };

                    // 匹配课程信息
                    let orArr = courseText.match(/(?<=actTeacherName.join\(','\),).*?(?=\);)/g);
                    if (!orArr || orArr.length === 0) {
                        console.warn("无法匹配课程信息:", courseText.substring(0, 100));
                        return;
                    }

                    // 匹配星期
                    let dayMatches = courseText.match(/(?<=index \=).*?(?=\*unitCount)/g);
                    let day = dayMatches ? distinct(dayMatches) : ["0"];

                    // 匹配节次
                    let sectionMatches = courseText.match(/(?<=unitCount\+).*?(?=;)/g);
                    let section = sectionMatches ? distinct(sectionMatches) : ["0"];

                    // 匹配教师名
                    let teacherMatches = courseText.match(/(?<=name:").*?(?=")/g);
                    let teacher = teacherMatches ? distinct(teacherMatches) : ["未知"];

                    console.log("解析结果:", {
                        "orArr": orArr ? orArr[0] : "null",
                        "day": day,
                        "section": section,
                        "teacher": teacher
                    });

                    // 解析课程详细信息
                    try {
                        let courseCon = orArr[0].split(/(?<="|l|e),(?="|n|a)/);

                        if (courseCon && courseCon.length >= 4) {
                            // 课程名称
                            course.courseName = courseCon[1] ? courseCon[1].replace(/"/g, "") : "未知课程";

                            // 教室
                            course.roomName = courseCon[3] ? courseCon[3].replace(/"/g, "") : "未知教室";

                            // 教师名
                            course.teacherName = teacher.join(",");

                            // 解析周次信息
                            if (courseCon.length > 4) {
                                let weekStr = courseCon[4].split(",")[0].replace('"', "");
                                weekStr.split("").forEach((em, index) => {
                                    if (em == 1) course.weeks.push(index);
                                });
                            }

                            // 如果没有周次，添加默认周次
                            if (course.weeks.length === 0) {
                                for (let i = 1; i <= 16; i++) {
                                    course.weeks.push(i);
                                }
                            }

                            // 星期
                            course.day = Number(day[0]) + 1;

                            // 节次
                            section.forEach(con => {
                                course.sections.push(Number(con) + 1);
                            });

                            console.log("解析后的课程:", course);
                            courseArr.push(course);
                        } else {
                            console.warn("课程信息数组长度不足:", courseCon);
                        }
                    } catch (e) {
                        console.error("解析课程详情失败:", e);
                    }
                } catch (e) {
                    console.error("解析单个课程失败:", e);
                }
            });

            if (courseArr.length === 0) {
                message = "解析后未获取到任何课程";
            }
        } else {
            message = "未获取到课表数据";
        }
    } catch (e) {
        console.error("处理课程数据失败:", e);
        message = e.message.slice(0, 50);
    }

    // 如果有错误信息，添加一门错误提示课程
    if (message.length !== 0) {
        courseArr = [{
            courseName: "获取课表数据出错",
            teacherName: "请联系QQ:2190638246",
            roomName: message,
            day: 1,
            weeks: [1],
            sections: [1, 2, 3]
        }];
    }

    console.log("最终课程数据:", courseArr);
    Android.onProgress("课表解析完成，共" + courseArr.length + "门课程");

    return JSON.stringify(courseArr);
}