/**
 * 安徽工程大学教务系统课表解析脚本
 * 整合自provider.js, parser.js和timer.js
 * 该脚本同时支持移动端和电脑模式
 */

// 检测脚本是否在桌面模式下运行
function isDesktopMode() {
    return navigator.userAgent.indexOf("Windows NT") !== -1 ||
        navigator.userAgent.indexOf("Macintosh") !== -1;
}

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

// 为桌面模式添加直接的URL导航函数
async function navigateToDesktopCoursePage() {
    // 检查当前页面是否为登录页
    if (window.location.href.includes("login") ||
        document.querySelector('form[action*="login"]') ||
        document.querySelector('input[name="username"]')) {

        console.log("当前页面似乎是登录页，请先登录");
        Android.onProgress("当前页面似乎是登录页，请先登录");
        return false;
    }

    // 如果已经在课表页面，直接返回
    if (window.location.href.includes("xskb_list.do") ||
        document.getElementById("kbcontent") ||
        document.querySelector(".kbcontent")) {
        console.log("已经在课表页面");
        Android.onProgress("已经在课表页面");
        return true;
    }

    // 检查主页的导航菜单
    const menuLinks = document.querySelectorAll('a[href*="xskb_list"], a:contains("课表"), a:contains("我的课表")');
    if (menuLinks.length > 0) {
        console.log("找到课表链接，尝试点击");
        Android.onProgress("找到课表链接，尝试导航");

        // 获取课表链接
        let courseLink = null;
        for (let i = 0; i < menuLinks.length; i++) {
            if (menuLinks[i].textContent.includes("课表") ||
                menuLinks[i].href.includes("xskb_list")) {
                courseLink = menuLinks[i].href;
                break;
            }
        }

        if (courseLink) {
            console.log("尝试导航到:", courseLink);
            Android.onProgress("正在导航到课表页面...");
            window.location.href = courseLink;
            return false; // 需要等待导航完成
        }
    }

    // 尝试直接构造课表URL
    const baseUrl = window.location.origin;
    if (window.location.hostname.includes("webvpn")) {
        // WebVPN环境
        console.log("WebVPN环境，尝试直接导航到课表页面");
        window.location.href = "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/xskb_list.do";
    } else {
        // 直连环境
        console.log("直连环境，尝试直接导航到课表页面");
        window.location.href = baseUrl + "/jsxsd/xskb/xskb_list.do";
    }

    return false; // 需要等待导航完成
}

// 修改桌面版课表解析函数，增强其识别能力
function parseDesktopCourseTable(domDoc) {
    try {
        console.log("使用桌面模式特定解析方法");
        Android.onProgress("使用桌面模式特定解析方法");

        const courses = [];

        // 尝试多种选择器找到课表表格
        const kbTableSelectors = [
            "#kbcontent table.kbcontent",
            "table.kbcontent",
            "#Table1",
            "#kbtable",
            "table[id*='kb']",
            "table[class*='kb']",
            "table" // 最后尝试任何表格
        ];

        let kbTable = null;
        for (const selector of kbTableSelectors) {
            const table = domDoc.querySelector(selector);
            if (table) {
                kbTable = table;
                console.log("找到课表表格，使用选择器:", selector);
                break;
            }
        }

        if (!kbTable) {
            console.error("找不到课表表格");
            Android.onProgress("找不到课表表格，尝试尝试其他方法");
            return [];
        }

        // 开发者工具输出表格结构，便于调试
        console.log("表格结构:", kbTable.outerHTML.substring(0, 200) + "...");

        // 获取表格行，跳过表头行
        const rows = kbTable.querySelectorAll("tr");
        console.log("表格总行数:", rows.length);

        // 确定表格的列数和行偏移
        let columnCount = 0;
        let headerRows = 0;

        // 检查前几行，确定表头和主体结构
        for (let i = 0; i < Math.min(3, rows.length); i++) {
            const cells = rows[i].querySelectorAll("td, th");
            if (cells.length > columnCount) {
                columnCount = cells.length;
            }

            // 检查是否是表头行
            const headerCells = rows[i].querySelectorAll("th");
            if (headerCells.length > 0 || cells.length <= 3) {
                headerRows++;
            }
        }

        console.log("表格列数:", columnCount, "表头行数:", headerRows);

        // 遍历表格中的课程单元格
        for (let rowIdx = headerRows; rowIdx < rows.length; rowIdx++) {
            const row = rows[rowIdx];
            const cells = row.querySelectorAll("td");

            // 计算当前行表示的节次
            // 一般来说，第一列是节次信息，所以从第二列开始是周一到周日的课程
            const timeCell = cells[0]; // 第一列通常是时间信息
            const timeText = timeCell ? timeCell.textContent.trim() : "";
            let startNode = 0;

            // 尝试从时间单元格提取节次信息
            const nodeMatch = timeText.match(/第(\d+)节/);
            if (nodeMatch) {
                startNode = parseInt(nodeMatch[1]);
            } else {
                // 如果无法从文本提取，则根据行索引推算
                startNode = (rowIdx - headerRows) * 2 + 1;
            }

            // 检查每个周几的单元格
            for (let colIdx = 1; colIdx < cells.length; colIdx++) {
                const cell = cells[colIdx];
                const cellContent = cell.textContent.trim();

                // 忽略空单元格或只有换行符的单元格
                if (!cellContent || cellContent === "&nbsp;" || cellContent === " ") continue;

                // 计算课程星期几 (1-7)
                const dayOfWeek = colIdx;

                // 解析课程信息
                // 尝试多种方式解析课程单元格内容
                parseCourseCellContent(cell, cellContent, dayOfWeek, startNode, courses);
            }
        }

        console.log("桌面模式解析到课程数:", courses.length);

        if (courses.length === 0) {
            // 如果没有找到课程，尝试其他方法
            // 可能是表格结构不常见，尝试直接解析HTML
            return parseUncommonDesktopTable(domDoc);
        }

        return resolveCourseConflicts(courses);
    } catch (error) {
        console.error("桌面模式解析课表出错:", error);
        Android.onProgress("桌面模式解析课表出错: " + error.message);
        return [];
    }
}

// 解析课程单元格内容
function parseCourseCellContent(cell, cellContent, day, startNode, courses) {
    // 尝试多种格式来分割课程信息

    // 情况1: 内容已按<br>换行分隔
    if (cell.innerHTML.includes("<br>") || cell.innerHTML.includes("<br/>")) {
        const parts = cell.innerHTML.split(/<br\s*\/?>/i);
        const cleanParts = parts.map(p => p.replace(/<[^>]*>/g, "").trim()).filter(p => p);

        if (cleanParts.length >= 1) {
            const name = cleanParts[0];
            let teacher = "";
            let position = "";
            let weekInfo = "";

            // 尝试识别周次信息、教师和地点
            for (let i = 1; i < cleanParts.length; i++) {
                const part = cleanParts[i];
                if (/周次|第\d+.+?周|^\d+[\-,]\d+周$/.test(part) ||
                    part.includes("周") && /\d+/.test(part)) {
                    weekInfo = part;
                } else if (part.includes("地点:") || part.includes("教室")) {
                    position = part.replace(/地点[:：]/, "").trim();
                } else if (part.includes("老师:") || part.includes("教师")) {
                    teacher = part.replace(/老师[:：]|教师[:：]/, "").trim();
                } else if (!teacher && i < 3) {
                    teacher = part; // 假设没找到明确标记的教师，但这是第二或第三行
                } else if (!position && i >= 2) {
                    position = part; // 假设这是地点信息
                }
            }

            // 创建并添加课程
            addCourseFromParsedInfo(name, teacher, position, weekInfo, day, startNode, 2, courses);
        }
        return;
    }

    // 情况2: 使用空格或换行符分隔的文本
    const lines = cellContent.split(/[\n\r\s]{2,}/).map(l => l.trim()).filter(l => l);

    if (lines.length >= 1) {
        const name = lines[0];
        let teacher = "";
        let position = "";
        let weekInfo = "";

        // 尝试从剩余行中提取信息
        for (let i = 1; i < lines.length; i++) {
            const line = lines[i];

            if (/周次|第\d+.+?周|^\d+[\-,]\d+周$/.test(line) ||
                line.includes("周") && /\d+/.test(line)) {
                weekInfo = line;
            } else if (line.includes("地点:") || line.includes("教室")) {
                position = line.replace(/地点[:：]|教室[:：]/, "").trim();
            } else if (line.includes("老师:") || line.includes("教师")) {
                teacher = line.replace(/老师[:：]|教师[:：]/, "").trim();
            } else if (!teacher && i === 1) {
                teacher = line;
            } else if (!position && i >= 2) {
                position = line;
            }
        }

        // 创建并添加课程
        addCourseFromParsedInfo(name, teacher, position, weekInfo, day, startNode, 2, courses);
    }
}

// 从解析的信息中创建课程对象并添加到课程列表
function addCourseFromParsedInfo(name, teacher, position, weekInfo, day, startNode, nodeLength, courses) {
    // 解析周次信息
    const weeks = parseWeekInfo(weekInfo);

    // 创建课程对象
    const course = {
        name: name,
        position: position || "未知地点",
        teacher: teacher || "未知教师",
        day: day,
        startNode: startNode,
        endNode: startNode + (nodeLength - 1),
        weeks: weeks,
        sections: Array.from({ length: nodeLength }, (_, i) => startNode + i)
    };

    courses.push(course);
    console.log("解析到课程:", course.name, "星期", course.day, "第", course.startNode, "-", course.endNode, "节");
}

// 解析周次信息
function parseWeekInfo(weekInfo) {
    let weeks = [];

    if (!weekInfo) {
        // 默认为1-16周
        weeks = Array.from({ length: 16 }, (_, i) => i + 1);
        return weeks;
    }

    // 提取周次范围（如"1-16周"或"1-16"）
    const rangeMatches = weekInfo.match(/(\d+)\s*[-–]\s*(\d+)(?:周)?/g) || [];
    for (const match of rangeMatches) {
        const [start, end] = match.replace(/周$/, "").split(/[-–]/).map(num => parseInt(num.trim()));
        if (!isNaN(start) && !isNaN(end)) {
            for (let i = start; i <= end; i++) {
                if (!weeks.includes(i)) {
                    weeks.push(i);
                }
            }
        }
    }

    // 提取单独的周次（如"1,3,5周"或"7"）
    const singleMatches = weekInfo.match(/(?<!\d[-–])\d+(?![-–]\d)/g) || [];
    for (const match of singleMatches) {
        const week = parseInt(match);
        if (!isNaN(week) && !weeks.includes(week)) {
            weeks.push(week);
        }
    }

    // 特殊模式：单周/双周
    if (weekInfo.includes("单周")) {
        weeks = weeks.filter(w => w % 2 === 1);
    } else if (weekInfo.includes("双周")) {
        weeks = weeks.filter(w => w % 2 === 0);
    }

    // 如果没有解析出周次，默认为1-16周
    if (weeks.length === 0) {
        weeks = Array.from({ length: 16 }, (_, i) => i + 1);
    }

    // 排序周次
    weeks.sort((a, b) => a - b);

    return weeks;
}

// 处理非常规桌面课表格式
function parseUncommonDesktopTable(domDoc) {
    console.log("尝试解析非常规桌面课表格式");
    Android.onProgress("尝试解析非常规桌面课表格式");

    const courses = [];

    // 尝试从页面中提取课程信息
    // 方法1: 寻找可能包含课程信息的div或表格
    const possibleContainers = domDoc.querySelectorAll("div[class*='kb'], div[id*='kb'], table");

    for (const container of possibleContainers) {
        // 检查是否包含课程信息
        const text = container.textContent;
        if (text.includes("课程") && text.includes("教师") && text.includes("周次")) {
            console.log("找到可能包含课程信息的容器:", container.tagName);

            // 尝试提取课程信息
            const courseItems = container.querySelectorAll("tr, div[class*='item'], li");
            for (const item of courseItems) {
                const itemText = item.textContent.trim();

                // 检查是否像是课程信息
                if (itemText.length > 10 && !itemText.includes("课表") && !itemText.includes("学期")) {
                    // 简单分割信息
                    const parts = itemText.split(/[,，]|\s{2,}|\n/);
                    if (parts.length >= 3) {
                        let name = parts[0].trim();
                        let teacher = "";
                        let position = "";
                        let day = 0;
                        let startNode = 0;
                        let endNode = 0;
                        let weekInfo = "";

                        // 尝试提取教师信息
                        for (const part of parts) {
                            if (part.includes("教师") || part.includes("老师")) {
                                teacher = part.replace(/教师[：:]/g, "").trim();
                            } else if (part.includes("地点") || part.includes("教室")) {
                                position = part.replace(/地点[：:]/g, "").trim();
                            } else if (part.includes("周") && /\d+/.test(part)) {
                                weekInfo = part;
                            } else if (part.includes("星期") || part.includes("周")) {
                                // 尝试提取星期几
                                const dayMatch = part.match(/星期([一二三四五六日])/);
                                if (dayMatch) {
                                    const dayStr = dayMatch[1];
                                    day = "一二三四五六日".indexOf(dayStr) + 1;
                                }
                            } else if (part.includes("节")) {
                                // 尝试提取节次
                                const nodesMatch = part.match(/第(\d+)[^\d]+(\d+)节/);
                                if (nodesMatch) {
                                    startNode = parseInt(nodesMatch[1]);
                                    endNode = parseInt(nodesMatch[2]);
                                }
                            }
                        }

                        if (name && day > 0 && startNode > 0) {
                            // 解析周次
                            const weeks = parseWeekInfo(weekInfo);

                            // 创建课程节次
                            const nodeLength = endNode - startNode + 1;
                            const sections = Array.from({ length: nodeLength },
                                (_, i) => startNode + i
                            );

                            // 创建课程对象
                            const course = {
                                name: name,
                                position: position || "未知地点",
                                teacher: teacher || "未知教师",
                                day: day,
                                startNode: startNode,
                                endNode: endNode,
                                weeks: weeks,
                                sections: sections
                            };

                            courses.push(course);
                        }
                    }
                }
            }
        }
    }

    console.log("非常规格式解析到课程数:", courses.length);
    return resolveCourseConflicts(courses);
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

// 解析课表
function parseSchedule(html) {
    try {
        console.log("开始解析课表HTML");
        Android.onProgress("开始解析课表HTML");

        // 桌面模式和移动模式可能有不同的课表格式
        if (isDesktopMode()) {
            // 桌面模式下尝试另一种解析方式
            const domDoc = textToDom(html);

            // 在桌面版中查找特定的课表容器
            const kbContentDiv = domDoc.getElementById("kbcontent");
            if (kbContentDiv) {
                console.log("找到桌面版课表内容");
                // 自定义解析桌面版课表
                return parseDesktopCourseTable(domDoc);
            }
        }

        // 提取课程数据 - 标准移动模式解析
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

        // 根据是否为桌面模式选择不同的解析逻辑
        if (isDesktopMode()) {
            return parseDesktopCourseTable(document);
        }

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

// 修改现有的getScheduleData函数，增加桌面模式支持
async function getScheduleData() {
    try {
        console.log("开始获取课表...");
        Android.onProgress("开始获取课表...");

        // 检查是否在WebVPN环境中
        let isWebVPN = window.location.hostname.includes("webvpn");
        let isDesktop = isDesktopMode();

        console.log("环境检测：", isWebVPN ? "WebVPN" : "直连", isDesktop ? "桌面模式" : "移动模式");
        Android.onProgress("环境检测：" + (isWebVPN ? "WebVPN" : "直连") + ", " + (isDesktop ? "桌面模式" : "移动模式"));

        let preUrl = window.location.origin;

        console.log("当前页面URL:", window.location.href);
        console.log("基础URL:", preUrl);
        Android.onProgress("当前页面: " + window.location.href);

        // 桌面模式优化处理
        if (isDesktop) {
            console.log("检测到桌面模式，尝试优化课表获取流程");
            Android.onProgress("桌面模式优化已启用");

            // 尝试直接导航到课表页面
            const isOnCoursePage = await navigateToDesktopCoursePage();

            // 如果成功导航到课表页面或已经在课表页面
            if (isOnCoursePage) {
                // 直接解析当前页面
                const courseResults = parseDesktopCourseTable(document);
                if (courseResults && courseResults.length > 0) {
                    Android.onScheduleData(JSON.stringify(courseResults));
                    return;
                }
            } else {
                Android.onProgress("请稍候，正在导航到课表页面...");
                // 给页面足够时间加载
                await sleep(2);
            }
        }

        // 在不同环境下调整URL
        let idurl = preUrl;
        if (window.location.href.includes("courseTableForStd") ||
            window.location.href.includes("xskb_list")) {
            idurl = window.location.href;
        } else {
            if (isWebVPN) {
                // 根据是否为桌面模式选择不同的URL
                if (isDesktop) {
                    // 桌面模式URL
                    idurl = "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/xskb_list.do";
                } else {
                    // 移动模式URL
                    idurl = "https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login";
                }
                console.log("使用WebVPN URL:", idurl);
                Android.onProgress("使用WebVPN URL: " + idurl);

                // 如果不在课表页面，尝试导航过去
                if (window.location.href !== idurl) {
                    window.location.href = idurl;
                    await sleep(2);
                }
            } else {
                // 非WebVPN环境
                idurl = preUrl + (isDesktop ? '/jsxsd/xskb/xskb_list.do' : '/courseTableForStd.action');
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
        let isCoursePage = courseTableCon.includes("courseTable") ||
            courseTableCon.includes("kbcontent") ||
            courseTableCon.includes("xskb_list");

        if (!isCoursePage) {
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

            Android.onError("未能识别课表页面内容，请确认已成功登录并进入课表页面");
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

        // 尝试不同的方式获取ID
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
                // 在桌面模式下尝试其他方式查找
                if (isDesktop) {
                    const desktopIdMatch = courseTableCon.match(/(?<=action="xskb_list.do\?xnm=).*?(?=&)/);
                    if (desktopIdMatch) {
                        ids = desktopIdMatch[0];
                        console.log("通过桌面模式特定方式找到ID:", ids);
                    }
                }

                if (!ids) {
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

            if (isDesktop) {
                // 桌面模式下的课表请求参数
                formData.set("xnm", semesterIds[i].substr(0, 4)); // 学年
                formData.set("xqm", semesterIds[i].substr(4)); // 学期
                formData.set("kklxdm", "01"); // 课程类型代码
            } else {
                // 移动模式下的课表请求参数
                formData.set("ignoreHead", 1);
                formData.set("setting.kind", "std");
                formData.set("startWeek", "");
                formData.set("semester.id", semesterIds[i]);
                formData.set("ids", ids);
            }

            let url;
            if (isWebVPN) {
                // 适应WebVPN环境的URL
                url = isDesktop ?
                    "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/xskb_list.do" :
                    "https://webvpn.ahpu.edu.cn/http/webvpn94ff67/jsxsd/xskb/courseTableForStd!courseTable.action";
            } else {
                // 直连环境的URL
                url = preUrl + (isDesktop ?
                    "/jsxsd/xskb/xskb_list.do" :
                    "/courseTableForStd!courseTable.action");
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

// 自动执行脚本
(function() {
    console.log("课表解析脚本已加载");
    Android.onProgress("课表解析脚本已加载 - 支持电脑模式");

    // 延迟执行，确保页面完全加载
    setTimeout(function() {
        console.log("开始自动执行课表解析");
        Android.onProgress("开始自动执行课表解析");
        getScheduleData();
    }, 1000);
})();