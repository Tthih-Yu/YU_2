/**
 * 安徽工程大学教务系统课表解析脚本
 * 整合自provider.js, parser.js和timer.js
 */

// 辅助函数 - SHA1编码
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

// 解析HTML到DOM
function textToDom(text) {
    let parser = new DOMParser();
    return parser.parseFromString(text, "text/html");
}

// HTTP请求函数
async function request(method, data, url) {
    try {
        console.log("发起请求:", url);
        Android.onProgress("发起请求: " + url);

        let response = await fetch(url, {
            method: method,
            body: data,
            credentials: 'include'
        });

        console.log("请求状态:", response.status);
        Android.onProgress("请求状态: " + response.status);

        if (!response.ok) {
            throw new Error("请求失败，状态码: " + response.status);
        }

        return await response.text();
    } catch (error) {
        console.error("请求出错:", error);
        Android.onProgress("请求出错: " + error.message);
        return null;
    }
}

// 睡眠函数
function sleep(seconds) {
    return new Promise(resolve => setTimeout(resolve, seconds * 1000));
}

// 获取学期ID
async function getSemestersId(preUrl, courseTableCon) {
    let semesterIds = [];

    // 调试输出
    console.log("获取学期ID, preUrl:", preUrl);
    console.log("courseTableCon前100字符:", courseTableCon.substring(0, 100));
    Android.onProgress("正在获取学期数据...");

    // 在WebVPN环境中，URL可能需要调整
    let xqurl = preUrl + "/dataQuery.action";
    if (preUrl.includes("webvpn")) {
        // WebVPN环境下可能需要特殊处理URL
        console.log("检测到WebVPN环境");
        Android.onProgress("检测到WebVPN环境");
    }

    let xqdata = new FormData();
    const tagIdMatch = courseTableCon.match(/(?<=semesterBar).*?(?=Semester)/);
    const valueMatch = courseTableCon.match(/(?<=value:").*?(?=")/);

    console.log("tagIdMatch:", tagIdMatch);
    console.log("valueMatch:", valueMatch);

    if (!tagIdMatch || !valueMatch) {
        console.error("无法匹配到学期信息");
        Android.onProgress("无法匹配到学期信息，尝试使用硬编码的学期ID");

        // 使用硬编码的默认学期ID
        return {
            'semesterIds': ['175', '174', '173', '172'], // 常见的学期ID
            'semesterIndex': 0
        };
    }

    xqdata.set("tagId", "semesterBar" + tagIdMatch[0] + "Semester");
    xqdata.set("dataType", "semesterCalendar");
    xqdata.set("value", valueMatch[0]);
    xqdata.set("empty", false);

    let currentYear = new Date().getFullYear();
    let response = await request("post", xqdata, xqurl);

    if (!response) {
        console.error("请求学期信息失败");
        Android.onProgress("请求学期信息失败，使用默认学期ID");

        // 使用硬编码的默认学期ID
        return {
            'semesterIds': ['175', '174', '173', '172'], // 常见的学期ID
            'semesterIndex': 0
        };
    }

    try {
        let semesters = JSON.parse(response).semesters;
        let count = 0;

        // 获取当前学年学期
        for (let key in semesters) {
            if (semesters[key][0].schoolYear.search(currentYear) != -1) {
                for (let key1 in semesters[key]) {
                    let semId = semesters[key][key1];
                    semesterIds.push(semesters[key][key1]['id']);
                    console.log("找到学期ID:", semesterIds[semesterIds.length - 1]);
                }
                if (++count == 2) break;
            }
        }
    } catch (e) {
        console.error("解析学期信息失败:", e);
        Android.onProgress("解析学期信息失败，使用默认学期ID");

        // 使用硬编码的默认学期ID
        return {
            'semesterIds': ['175', '174', '173', '172'], // 常见的学期ID
            'semesterIndex': 0
        };
    }

    if (semesterIds.length === 0) {
        console.warn("未找到学期ID，使用默认值");
        Android.onProgress("未找到学期ID，使用默认值");
        semesterIds = ['175', '174', '173', '172']; // 常见的学期ID
    }

    return {
        'semesterIds': semesterIds,
        'semesterIndex': 0 // 默认使用第一个学期
    };
}

// 获取课表数据
async function getScheduleData() {
    try {
        console.log("开始获取课表...");
        Android.onProgress("开始获取课表...");

        // 检查是否在WebVPN环境中
        let isWebVPN = window.location.hostname.includes("webvpn");
        let preUrl = window.location.origin;

        console.log("当前页面URL:", window.location.href);
        console.log("基础URL:", preUrl);
        Android.onProgress("当前页面: " + window.location.href);

        // 在WebVPN环境下，我们可能需要调整URL
        let idurl = preUrl;
        if (window.location.href.includes("courseTableForStd")) {
            idurl = window.location.href;
        } else {
            idurl = preUrl + '/courseTableForStd.action';
            if (isWebVPN) {
                // 尝试直接跳转到WebVPN的课表页面
                idurl = "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/xskb_list.do";
                console.log("使用WebVPN课表URL:", idurl);
                Android.onProgress("使用WebVPN课表URL");

                // 如果不在课表页面，尝试导航过去
                window.location.href = idurl;
                await sleep(2);
            }
        }

        // 获取课表页面数据
        let courseTableCon = document.documentElement.outerHTML;
        console.log("获取的页面内容长度:", courseTableCon.length);
        Android.onProgress("获取页面内容，长度: " + courseTableCon.length);

        if (!courseTableCon || courseTableCon.length < 1000) {
            console.error("获取课表页面失败或内容不完整");
            Android.onError("获取课表页面失败或内容不完整");
            return;
        }

        // 检查是否成功进入课表页面
        if (!courseTableCon.includes("courseTable") && !courseTableCon.includes("kbcontent")) {
            console.error("未能识别课表页面内容");
            Android.onProgress("未识别到课表内容，尝试直接解析页面");

            // 尝试直接从页面解析课表
            let tableEls = document.querySelectorAll("table");
            console.log("页面中的表格数量:", tableEls.length);

            // 检查是否有表格，可能包含课表
            if (tableEls.length > 0) {
                // 尝试从DOM直接解析课表
                const courseResults = parseScheduleFromDOM();
                if (courseResults && courseResults.length > 0) {
                    console.log("从DOM直接解析课表成功:", courseResults);
                    Android.onProgress("从页面直接解析课表成功");
                    Android.onScheduleData(JSON.stringify(courseResults));
                    return;
                }
            }

            Android.onError("未能识别课表页面内容，请确认已成功登录");
            return;
        }

        // 获取学期ID
        let semIdsJson = await getSemestersId(preUrl, courseTableCon);

        if (!semIdsJson) {
            console.error("获取学期信息失败");
            Android.onError("获取学期信息失败");
            return;
        }

        const idsMatch = courseTableCon.match(/(?<=bg.form.addInput\(form,"ids",").*?(?="\);)/);

        // 如果无法获取ID，尝试其他方式
        let ids = "";
        if (!idsMatch) {
            console.warn("未匹配到用户ID，尝试其他方式获取");
            Android.onProgress("尝试其他方式获取用户ID");

            // 尝试从页面中找到其他可能的ID
            const otherIdMatch = courseTableCon.match(/(?<=input.+?name="ids".+?value=").*?(?=")/);
            if (otherIdMatch) {
                ids = otherIdMatch[0];
                console.log("通过其他方式找到ID:", ids);
            } else {
                console.error("无法获取用户ID");
                Android.onProgress("无法获取用户ID，尝试使用默认解析方法");

                // 尝试直接解析当前页面上的课表
                const courseResults = parseSchedule(courseTableCon);
                if (courseResults && courseResults.length > 0) {
                    console.log("直接解析当前页面课表成功:", courseResults);
                    Android.onProgress("直接解析当前页面课表成功");
                    Android.onScheduleData(JSON.stringify(courseResults));
                    return;
                } else {
                    Android.onError("无法获取用户ID且解析失败");
                    return;
                }
            }
        } else {
            ids = idsMatch[0];
        }

        // 获取课表数据
        let courseResults = [];
        let i = semIdsJson.semesterIndex;
        let semesterIds = semIdsJson.semesterIds;

        while (i < semesterIds.length) {
            await sleep(0.4);
            Android.onProgress("正在获取第" + (i + 1) + "个学期的课表...");

            let formData = new FormData();
            formData.set("ignoreHead", 1);
            formData.set("setting.kind", "std");
            formData.set("startWeek", "");
            formData.set("semester.id", semesterIds[i]);
            formData.set("ids", ids);

            let url = preUrl + "/courseTableForStd!courseTable.action";
            if (isWebVPN) {
                // 适应WebVPN环境的URL
                url = "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/xskb_list.do";
            }

            console.log("请求课表URL:", url);
            Android.onProgress("请求课表: " + url);

            let response = await request("post", formData, url);

            if (response) {
                console.log("获取到课表响应，长度:", response.length);
                Android.onProgress("获取到课表响应，长度: " + response.length);

                // 解析课表数据
                courseResults = parseSchedule(response);
                if (courseResults && courseResults.length > 0) {
                    console.log("解析到课程数量:", courseResults.length);
                    break;
                }
            }

            i++;
        }

        if (!courseResults || courseResults.length === 0) {
            console.error("未找到课表数据");
            Android.onError("未找到课表数据");

            // 最后尝试从DOM直接解析
            const domResults = parseScheduleFromDOM();
            if (domResults && domResults.length > 0) {
                console.log("从DOM解析课表成功:", domResults);
                Android.onProgress("从DOM解析课表成功");
                Android.onScheduleData(JSON.stringify(domResults));
                return;
            }

            return;
        }

        // 返回解析后的课表数据
        console.log("课表数据获取成功:", courseResults);
        Android.onProgress("课表数据获取成功，共" + courseResults.length + "门课程");
        Android.onScheduleData(JSON.stringify(courseResults));

    } catch (error) {
        console.error("获取课表失败:", error);
        Android.onError("获取课表失败: " + error.message);
    }
}

// 解析课表
function parseSchedule(html) {
    try {
        console.log("开始解析课表HTML");
        Android.onProgress("开始解析课表HTML");

        // 提取课程数据
        const courseDataPattern = /(?<=var\s+kbxx_id_.*?\[\d+\]\s*=\s*{).*?(?=};)/gs;
        const matches = html.matchAll(courseDataPattern);

        let courses = [];
        let matchCount = 0;

        for (const match of matches) {
            matchCount++;
            if (match[0]) {
                // 解析课程属性
                const propPattern = /(\w+):"([^"]*)"/g;
                let courseObj = {};
                let propMatch;

                while (propMatch = propPattern.exec(match[0])) {
                    courseObj[propMatch[1]] = propMatch[2];
                }

                // 只处理有效的课程数据
                if (courseObj.name && courseObj.teacher && courseObj.location && courseObj.week && courseObj.day && courseObj.start && courseObj.step) {
                    // 解析周次
                    let weekPattern = /\d+/g;
                    let weeks = [];
                    let weekMatch;

                    while (weekMatch = weekPattern.exec(courseObj.week)) {
                        weeks.push(parseInt(weekMatch[0]));
                    }

                    // 解析节次
                    let startNode = parseInt(courseObj.start);
                    let endNode = startNode + parseInt(courseObj.step) - 1;

                    // 创建课程对象
                    let course = {
                        name: courseObj.name,
                        position: courseObj.location,
                        teacher: courseObj.teacher,
                        day: parseInt(courseObj.day),
                        startNode: startNode,
                        endNode: endNode,
                        weeks: weeks,
                        sections: Array.from({ length: endNode - startNode + 1 }, (_, i) => startNode + i)
                    };

                    courses.push(course);
                }
            }
        }

        console.log("解析匹配数:", matchCount, "有效课程数:", courses.length);
        Android.onProgress("解析匹配数: " + matchCount + ", 有效课程数: " + courses.length);

        if (courses.length > 0) {
            return resolveCourseConflicts(courses);
        } else {
            // 如果没有找到课程，尝试从DOM解析
            console.log("通过正则解析未找到课程，尝试从DOM解析");
            Android.onProgress("通过正则未找到课程，尝试从DOM解析");
            return parseScheduleFromDOM();
        }
    } catch (error) {
        console.error("解析课表出错:", error);
        Android.onProgress("解析课表出错: " + error.message);
        return [];
    }
}

// 从DOM直接解析课表
function parseScheduleFromDOM() {
    try {
        console.log("尝试从DOM解析课表");
        Android.onProgress("尝试从DOM解析课表");

        let courses = [];
        let tableElement = document.getElementById("kbcontent");

        // 如果找不到标准的课表元素，查找任何可能的表格
        if (!tableElement) {
            tableElement = document.querySelector("table");
        }

        if (!tableElement) {
            console.error("在DOM中未找到课表");
            Android.onProgress("在DOM中未找到课表");
            return [];
        }

        // 获取所有包含课程信息的格子
        let courseCells = tableElement.querySelectorAll("td[rowspan]");
        console.log("找到课程单元格数量:", courseCells.length);

        for (let i = 0; i < courseCells.length; i++) {
            let cell = courseCells[i];
            let content = cell.textContent.trim();

            // 忽略空单元格
            if (!content) continue;

            // 尝试解析课程信息
            let rowspan = parseInt(cell.getAttribute("rowspan") || "1");
            let dayIndex = -1;

            // 确定这个单元格是周几的课
            for (let j = 0; j < cell.parentElement.children.length; j++) {
                if (cell.parentElement.children[j] === cell) {
                    dayIndex = j;
                    break;
                }
            }

            // 如果无法确定周几，跳过
            if (dayIndex < 1) continue;

            // 计算课程开始节次 (需要基于实际表格结构调整)
            let startNode = 1;
            let prevRow = cell.parentElement.previousElementSibling;
            while (prevRow) {
                if (prevRow.children.length > 1) {
                    startNode++;
                }
                prevRow = prevRow.previousElementSibling;
            }

            // 解析课程名称、教师和地点
            let lines = content.split("\n").filter(line => line.trim());

            if (lines.length >= 1) {
                let name = lines[0].trim();
                let teacher = "";
                let position = "";

                if (lines.length >= 2) {
                    // 检查是否包含教师信息
                    if (lines[1].includes("：")) {
                        teacher = lines[1].split("：")[1].trim();
                    } else {
                        teacher = lines[1].trim();
                    }
                }

                if (lines.length >= 3) {
                    // 检查是否包含教室信息
                    if (lines[2].includes("：")) {
                        position = lines[2].split("：")[1].trim();
                    } else {
                        position = lines[2].trim();
                    }
                }

                // 为简单起见，假设课程在所有周都有
                let weeks = Array.from({ length: 16 }, (_, i) => i + 1);

                // 创建课程对象
                let course = {
                    name: name,
                    position: position,
                    teacher: teacher,
                    day: dayIndex,
                    startNode: startNode,
                    endNode: startNode + rowspan - 1,
                    weeks: weeks,
                    sections: Array.from({ length: rowspan }, (_, i) => startNode + i)
                };

                courses.push(course);
            }
        }

        console.log("从DOM解析到课程数量:", courses.length);
        Android.onProgress("从DOM解析到课程数量: " + courses.length);

        return resolveCourseConflicts(courses);
    } catch (error) {
        console.error("从DOM解析课表出错:", error);
        Android.onProgress("从DOM解析课表出错: " + error.message);
        return [];
    }
}

// 处理课程冲突
function resolveCourseConflicts(parsedCourses) {
    let splitTag = "&";
    let allResultSet = new Set();

    parsedCourses.forEach(course => {
        course.weeks.forEach(week => {
            course.sections.forEach(section => {
                let parsedCourse = {
                    sections: [],
                    weeks: [],
                    name: course.name,
                    teacher: course.teacher,
                    position: course.position,
                    day: course.day
                };
                parsedCourse.weeks.push(week);
                parsedCourse.sections.push(section);
                allResultSet.add(JSON.stringify(parsedCourse));
            });
        });
    });

    let allResult = JSON.parse("[" + Array.from(allResultSet).toString() + "]").sort(function(a, b) {
        return (a.day - b.day) || (a.sections[0] - b.sections[0]);
    });

    let contractedResult = [];
    while (allResult.length !== 0) {
        let firstCourse = allResult.shift();
        if (firstCourse === undefined) continue;
        let weekTag = firstCourse.day;

        for (let i = 0; allResult[i] !== undefined && weekTag === allResult[i].day; i++) {
            if (firstCourse.weeks[0] === allResult[i].weeks[0]) {
                if (firstCourse.sections[0] === allResult[i].sections[0]) {
                    let index = firstCourse.name.split(splitTag).indexOf(allResult[i].name);
                    if (index === -1) {
                        firstCourse.name += splitTag + allResult[i].name;
                        firstCourse.teacher += splitTag + allResult[i].teacher;
                        firstCourse.position += splitTag + allResult[i].position;
                        firstCourse.position = firstCourse.position.replace(/undefined/g, '');
                        allResult.splice(i, 1);
                        i--;
                    } else {
                        let teachers = firstCourse.teacher.split(splitTag);
                        let positions = firstCourse.position.split(splitTag);
                        teachers[index] = teachers[index] === allResult[i].teacher ? teachers[index] : teachers[index] + "," + allResult[i].teacher;
                        positions[index] = positions[index] === allResult[i].position ? positions[index] : positions[index] + "," + allResult[i].position;
                        firstCourse.teacher = teachers.join(splitTag);
                        firstCourse.position = positions.join(splitTag);
                        firstCourse.position = firstCourse.position.replace(/undefined/g, '');
                        allResult.splice(i, 1);
                        i--;
                    }
                }
            }
        }
        contractedResult.push(firstCourse);
    }

    let finallyResult = [];
    while (contractedResult.length !== 0) {
        let firstCourse = contractedResult.shift();
        if (firstCourse === undefined) continue;
        let weekTag = firstCourse.day;

        for (let i = 0; contractedResult[i] !== undefined && weekTag === contractedResult[i].day; i++) {
            if (firstCourse.weeks[0] === contractedResult[i].weeks[0] &&
                firstCourse.name === contractedResult[i].name &&
                firstCourse.position === contractedResult[i].position &&
                firstCourse.teacher === contractedResult[i].teacher) {
                if (firstCourse.sections[firstCourse.sections.length - 1] + 1 === contractedResult[i].sections[0]) {
                    firstCourse.sections.push(contractedResult[i].sections[0]);
                    contractedResult.splice(i, 1);
                    i--;
                } else {
                    break;
                }
            }
        }
        finallyResult.push(firstCourse);
    }

    contractedResult = JSON.parse(JSON.stringify(finallyResult));
    finallyResult.length = 0;
    while (contractedResult.length !== 0) {
        let firstCourse = contractedResult.shift();
        if (firstCourse === undefined) continue;
        let weekTag = firstCourse.day;

        for (let i = 0; contractedResult[i] !== undefined && weekTag === contractedResult[i].day; i++) {
            if (firstCourse.sections.sort((a, b) => a - b).toString() ===
                contractedResult[i].sections.sort((a, b) => a - b).toString() &&
                firstCourse.name === contractedResult[i].name &&
                firstCourse.position === contractedResult[i].position &&
                firstCourse.teacher === contractedResult[i].teacher) {
                firstCourse.weeks.push(contractedResult[i].weeks[0]);
                contractedResult.splice(i, 1);
                i--;
            }
        }
        finallyResult.push(firstCourse);
    }

    console.log("最终处理后的课程数据:", finallyResult);
    return finallyResult;
}

// 自动执行脚本
(function() {
    console.log("课表解析脚本已加载");
    Android.onProgress("课表解析脚本已加载");

    // 延迟执行，确保页面完全加载
    setTimeout(function() {
        console.log("开始自动执行课表解析");
        Android.onProgress("开始自动执行课表解析");
        getScheduleData();
    }, 1000);
})();