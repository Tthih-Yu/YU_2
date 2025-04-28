#!/usr/bin/env python
# -*- coding: utf-8 -*-
# ------------------------------------------------
# 校园卡交易记录查询工具
# 版本: 1.0.0
# 创建时间: 2025-04-21
# 说明: 此工具用于自动登录校园卡系统，获取交易流水
#      并支持批量导出多页交易记录数据
# ------------------------------------------------

import sys
import os
import time
import math
import csv
from datetime import datetime
from PySide6.QtWidgets import (QApplication, QMainWindow, QVBoxLayout, 
                               QWidget, QPushButton, QHBoxLayout,
                               QLabel, QSpinBox, QProgressBar, QFileDialog,
                               QCheckBox, QMessageBox, QComboBox, QInputDialog,
                               QLineEdit, QGroupBox, QTextEdit)
from PySide6.QtWebEngineWidgets import QWebEngineView
from PySide6.QtWebEngineCore import QWebEngineProfile, QWebEngineCookieStore
from PySide6.QtCore import QUrl, Slot, QTimer, Qt, Signal, QThread
from PySide6.QtNetwork import QNetworkCookie
import requests
import json

LOGIN_URL = "http://220.178.164.65:8053/"
TARGET_DOMAIN = "220.178.164.65"
API_URL = "http://220.178.164.65:8053"
DEFAULT_IMEI = "7b3672f7e9efc1603b65203bd471162c"

class DataFetchThread(QThread):
    """线程类用于后台获取数据，避免UI卡顿"""
    progress_update = Signal(int, int)  # 信号：当前页码，总页数
    data_received = Signal(list)  # 信号：接收到的数据
    fetch_complete = Signal(bool, str)  # 信号：完成状态，消息
    
    def __init__(self, cookies, source_type, account, start_page, end_page):
        super().__init__()
        self.cookies = cookies
        self.source_type = source_type
        self.account = account
        self.start_page = start_page
        self.end_page = end_page
        self.all_data = []
        self.is_running = True
        
    def run(self):
        try:
            all_records = []
            total_pages = 0
            
            for page in range(self.start_page, self.end_page + 1):
                if not self.is_running:
                    break
                    
                success, data = fetch_transactions(
                    self.cookies, 
                    page=page,
                    source_type=self.source_type,
                    account=self.account,
                    silent=True
                )
                
                if success and data and 'rows' in data:
                    records = data['rows']
                    all_records.extend(records)
                    
                    # 只在第一页计算总页数
                    if page == self.start_page and 'total' in data:
                        total_records = int(data['total'])
                        records_per_page = len(records)
                        if records_per_page > 0:
                            total_pages = math.ceil(total_records / records_per_page)
                            # 更新结束页面，如果用户想要获取所有页面（end_page=0）
                            if self.end_page == 0 or self.end_page > total_pages:
                                self.end_page = total_pages
                    
                    self.progress_update.emit(page, self.end_page)
                    # 每批次数据发送回主线程
                    self.data_received.emit(records)
                    
                    # 避免请求过快
                    if page < self.end_page:
                        time.sleep(0.5)
                else:
                    self.fetch_complete.emit(False, f"第 {page} 页数据获取失败")
                    return
            
            self.all_data = all_records
            self.fetch_complete.emit(True, f"成功获取 {len(all_records)} 条交易记录")
        
        except Exception as e:
            self.fetch_complete.emit(False, f"数据获取出错: {str(e)}")
    
    def stop(self):
        self.is_running = False

# 统一的函数用于获取交易流水，这样它就不是类的一部分且可以单独调用
def fetch_transactions(cookies_dict, page=1, source_type=None, account=None, silent=False, parent_window=None):
    """
    使用获取到的Cookie尝试获取交易流水
    
    Args:
        cookies_dict: Cookie字典
        page: 页码
        source_type: 类型标识
        account: 账号
        silent: 是否静默模式（不打印详细信息）
        parent_window: 父窗口，用于显示对话框
    """
    if not silent:
        print("\n开始获取交易流水...")
    
    # 获取登录用户名（学号）
    if not account:
        for name, value in cookies_dict.items():
            # 登录成功后浏览器通常会获取用户信息，存储在某个Cookie中
            if 'account' in name.lower() or 'username' in name.lower() or 'sno' in name.lower():
                account = value
        
        # 如果没有在Cookie中找到账号，使用GUI对话框请求用户输入 (不再使用命令行输入)
        if not account and parent_window:
            account, ok = QInputDialog.getText(
                parent_window, 
                "输入学号", 
                "请输入您的学号:", 
                QLineEdit.Normal
            )
            if not ok or not account:
                if not silent:
                    print("用户取消了输入学号或输入为空")
                return False, None
    
    # 请求头
    headers = {
        'User-Agent': 'Mozilla/5.0 (Linux; Android 15; 23127PN0CC Build/AQ3A.240627.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.37 Mobile Safari/537.36',
        'Accept': 'application/json, text/javascript, */*; q=0.01',
        'Accept-Language': 'zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7',
        'Accept-Encoding': 'gzip, deflate',
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest',
        'Origin': API_URL,
        'Referer': f"{API_URL}/PPage/ComePage?flowID=15"
    }
    
    # 请求参数 - 使用更新的参数格式
    transaction_data = {
        'account': account,  # 使用account参数而不是sno
        'page': page,
        'json': 'true'
    }
    
    # 添加source_type作为参数如果有提供的话
    if source_type:
        transaction_data['sourcetype'] = source_type
    
    try:
        if not silent:
            print(f"请求参数: 账号={account}, 页码={page}, sourcetype={source_type or '未设置'}")
            print(f"使用Cookie: {cookies_dict}")
        
        # 发送请求
        response = requests.post(
            f"{API_URL}/Report/GetPersonTrjn",
            headers=headers,
            data=transaction_data,
            cookies=cookies_dict,
            timeout=15
        )
        
        # 检查响应
        if response.status_code == 200:
            try:
                if not silent:
                    # 尝试解析JSON
                    response_text = response.text
                    print(f"接收到的原始响应: {response_text[:200]}...")  # 显示前200个字符帮助调试
                
                # 解析JSON响应
                data = response.json()
                if isinstance(data, dict) and 'rows' in data:
                    if not silent:
                        print(f"成功获取到 {len(data['rows'])} 条交易记录!")
                        print(f"总记录数: {data.get('total', '未知')}")
                        
                        # 显示最近几条交易
                        print("\n最近的交易记录:")
                        for i, record in enumerate(data['rows'][:5]):  # 显示前5条
                            print(f"{i+1}. 时间: {record.get('OCCTIME', '未知')}, "
                                f"金额: {record.get('TRANAMT', '未知')}, "
                                f"余额: {record.get('CARDBAL', '未知')}, "
                                f"类型: {record.get('MERCNAME', '未知')}")
                    return True, data
                else:
                    if not silent:
                        print(f"响应格式不符: {data}")
                    return False, None
            except json.JSONDecodeError:
                if not silent:
                    print(f"解析JSON失败: {response.text[:200]}...")
                return False, None
        else:
            if not silent:
                print(f"请求失败: HTTP {response.status_code}")
                print(f"响应内容: {response.text[:200]}...")
            return False, None
    except Exception as e:
        if not silent:
            print(f"请求出错: {e}")
        return False, None

def save_transactions_to_csv(data, filename):
    """保存交易记录到CSV文件"""
    if not data:
        return False
    
    try:
        with open(filename, 'w', newline='', encoding='utf-8-sig') as f:
            # 确定所有可能的字段
            all_fields = set()
            for record in data:
                all_fields.update(record.keys())
            
            # 创建CSV写入器
            writer = csv.DictWriter(f, fieldnames=sorted(all_fields))
            writer.writeheader()
            writer.writerows(data)
        return True
    except Exception as e:
        print(f"保存CSV出错: {e}")
        return False

def save_transactions_to_json(data, filename):
    """保存交易记录到JSON文件"""
    if not data:
        return False
    
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        return True
    except Exception as e:
        print(f"保存JSON出错: {e}")
        return False

class LoginWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("校园卡登录与交易查询")
        self.setGeometry(100, 100, 950, 700)
        
        # Dict to store cookies as they come in
        self.stored_cookies = {}
        # 存储所有获取到的交易记录
        self.all_transaction_records = []
        # 数据获取线程
        self.fetch_thread = None
        # 存储关键的请求参数
        self.current_session_id = None
        self.current_source_type = None
        self.current_account = None

        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        # Change main layout to Horizontal
        main_layout = QHBoxLayout(central_widget)

        # --- Left Panel (Webview + Controls) ---
        left_panel_widget = QWidget()
        left_layout = QVBoxLayout(left_panel_widget)
        left_layout.setContentsMargins(0, 0, 0, 0) # No margins for the container

        # --- Web View ---
        self.webview = QWebEngineView()
        left_layout.addWidget(self.webview, 1) # Give webview more space in the left panel

        # Setup Profile and cookie monitoring
        self.profile = self.webview.page().profile()
        self.profile.setPersistentCookiesPolicy(QWebEngineProfile.PersistentCookiesPolicy.AllowPersistentCookies)
        self.cookie_store = self.profile.cookieStore()
        
        # Connect to the cookieAdded signal instead of using getAllCookies
        print("Connecting to cookieAdded signal...")
        self.cookie_store.cookieAdded.connect(self.on_cookie_added)
        
        # --- Control Area ---
        control_area = QWidget()
        control_layout = QVBoxLayout(control_area)
        control_layout.setContentsMargins(5, 5, 5, 5) # Add some margin
        left_layout.addWidget(control_area)

        # --- Login & Cookie Group ---
        login_group = QGroupBox("第一步: 登录与提取凭据")
        login_group_layout = QVBoxLayout(login_group)

        self.fetch_button = QPushButton("我已在上方页面登录成功，点击此处提取凭据")
        self.fetch_button.setToolTip("登录成功后点击这里，程序会自动尝试提取必要的访问凭据。")
        self.fetch_button.clicked.connect(self.use_stored_cookies)
        login_group_layout.addWidget(self.fetch_button)
        
        account_layout = QHBoxLayout()
        self.account_label = QLabel("当前学号:")
        self.account_input = QLabel("<i>(请先提取凭据)</i>") # Placeholder text
        account_layout.addWidget(self.account_label)
        account_layout.addWidget(self.account_input)
        account_layout.addStretch()
        login_group_layout.addLayout(account_layout)
        
        control_layout.addWidget(login_group)

        # --- Data Fetching Group ---
        fetch_group = QGroupBox("第二步: 获取交易记录")
        fetch_group_layout = QVBoxLayout(fetch_group)

        fetch_options_layout = QHBoxLayout()
        
        # Page range selection
        self.start_page_label = QLabel("开始页:")
        self.start_page_input = QSpinBox()
        self.start_page_input.setMinimum(1)
        self.start_page_input.setValue(1)
        self.start_page_input.setToolTip("设置要获取的交易记录的起始页码。")
        
        self.end_page_label = QLabel("结束页:")
        self.end_page_input = QSpinBox()
        self.end_page_input.setMinimum(0) # 0 means all pages
        self.end_page_input.setMaximum(999) # Initial max, will be updated
        self.end_page_input.setValue(0)
        self.end_page_input.setSpecialValueText("所有页")
        self.end_page_input.setToolTip("设置要获取的交易记录的结束页码 (设为 '所有页' 来获取全部记录)。")

        fetch_options_layout.addWidget(self.start_page_label)
        fetch_options_layout.addWidget(self.start_page_input)
        fetch_options_layout.addSpacing(20)
        fetch_options_layout.addWidget(self.end_page_label)
        fetch_options_layout.addWidget(self.end_page_input)
        fetch_options_layout.addStretch() # Push buttons to the right

        # Action buttons
        self.fetch_data_button = QPushButton("开始获取")
        self.fetch_data_button.setEnabled(False)
        self.fetch_data_button.setToolTip("提取凭据成功后，点击此按钮开始获取指定范围的交易记录。")
        self.fetch_data_button.clicked.connect(self.start_data_fetch)
        
        self.stop_fetch_button = QPushButton("停止获取")
        self.stop_fetch_button.setEnabled(False)
        self.stop_fetch_button.setToolTip("在获取过程中点击此按钮可以提前中止。")
        self.stop_fetch_button.clicked.connect(self.stop_data_fetch)
        
        fetch_options_layout.addWidget(self.fetch_data_button)
        fetch_options_layout.addWidget(self.stop_fetch_button)
        
        fetch_group_layout.addLayout(fetch_options_layout)

        # Progress Bar and Status
        self.progress_layout = QHBoxLayout()
        self.progress_label = QLabel("进度:")
        self.progress_bar = QProgressBar()
        self.progress_bar.setRange(0, 100)
        self.progress_bar.setValue(0)
        self.progress_bar.setTextVisible(True) # Show percentage text
        
        self.status_label = QLabel("状态: 请先在上方网页中登录")
        self.status_label.setStyleSheet("font-style: italic; color: gray;") # Initial style

        self.progress_layout.addWidget(self.progress_label)
        self.progress_layout.addWidget(self.progress_bar, 1) # Give progress bar more space
        fetch_group_layout.addLayout(self.progress_layout)
        fetch_group_layout.addWidget(self.status_label) # Status below progress bar
        
        control_layout.addWidget(fetch_group)

        # --- Saving Group ---
        save_group = QGroupBox("第三步: 保存数据")
        save_group_layout = QHBoxLayout(save_group) # Horizontal layout for saving

        self.save_label = QLabel("格式:")
        self.save_format_combo = QComboBox()
        self.save_format_combo.addItems(["CSV", "JSON"])
        self.save_format_combo.setToolTip("选择保存交易记录的文件格式。")

        self.save_button = QPushButton("保存记录")
        self.save_button.setEnabled(False)
        self.save_button.setToolTip("获取数据后，点击这里将记录保存到文件。")
        self.save_button.clicked.connect(self.save_data)

        self.records_count_label = QLabel("已获取 0 条记录")
        
        save_group_layout.addWidget(self.save_label)
        save_group_layout.addWidget(self.save_format_combo)
        save_group_layout.addWidget(self.save_button)
        save_group_layout.addStretch()
        save_group_layout.addWidget(self.records_count_label)

        control_layout.addWidget(save_group)

        # --- Final Setup ---
        # Make sure the webview takes up most space
        main_layout.setStretchFactor(self.webview, 1)
        main_layout.setStretchFactor(control_area, 0)
        left_layout.addWidget(control_area) # Add control area below webview

        # --- Right Panel (Instructions) ---
        right_panel_widget = QWidget()
        right_layout = QVBoxLayout(right_panel_widget)
        right_layout.setContentsMargins(5, 5, 5, 5)
        
        instructions_label = QLabel("<b>快速使用步骤:</b>")
        right_layout.addWidget(instructions_label)

        self.instructions_text = QTextEdit()
        self.instructions_text.setReadOnly(True)
        instructions_content = """
<ol>
    <li>🖱️ 双击运行 <code>启动交易查询工具.bat</code></li>
    <li>💻 在打开的 <b>左侧网页界面</b> 中登录系统 (学号 + 小灵龙密码)。</li>
    <li>✅ 登录成功后，点击下方的 <b>[我已在上方页面登录成功...]</b> 按钮。</li>
    <li>⌨️ 在弹出的窗口中输入你的 <b>校园卡卡号 (非学号)</b> 并确定。<br>
        <small><i>卡号查询: 小灵龙APP -> 应用中心 -> 主副卡转账 -> 主卡账号 (5位数字)</i></small>
    </li>
    <li>🔢 设置要获取的 <b>开始页</b> 和 <b>结束页</b> (选择"所有页"获取全部)。</li>
    <li>🖱️ 点击 <b>[开始获取]</b> 按钮开始下载数据。</li>
    <li>💾 获取完成后，选择 <b>格式 (CSV/JSON)</b> 并点击 <b>[保存记录]</b>。</li>
</ol>

<b>注意事项:</b>
<ul>
    <li>请勿频繁重复获取。</li>
    <li>妥善保管导出的文件。</li>
</ul>
        """
        self.instructions_text.setHtml(instructions_content)
        right_layout.addWidget(self.instructions_text)

        # --- Add panels to main layout ---
        main_layout.addWidget(left_panel_widget, 3) # Give left panel more space (ratio 3:1)
        main_layout.addWidget(right_panel_widget, 1)

        self.webview.setUrl(QUrl(LOGIN_URL))
        print("窗口已初始化，正在加载登录页面...")

    @Slot(QNetworkCookie)
    def on_cookie_added(self, cookie):
        """Store cookies as they are added by the website"""
        try:
            cookie_domain = cookie.domain().strip('.')
            if cookie_domain == TARGET_DOMAIN:
                cookie_name = cookie.name().data().decode()
                cookie_value = cookie.value().data().decode()
                self.stored_cookies[cookie_name] = cookie_value
                print(f"Cookie 已捕获: {cookie_name}")
        except Exception as e:
            print(f"处理 Cookie 时出错: {e}")

    @Slot()
    def use_stored_cookies(self):
        """Process the cookies we've stored from the cookieAdded signal"""
        print("-" * 40)
        print(f"检查已存储的 Cookies，当前共有 {len(self.stored_cookies)} 个")
        
        # 检查重要的cookie值
        session_id = self.stored_cookies.get("ASP.NET_SessionId")
        hallticket = self.stored_cookies.get("hallticket")
        
        if not session_id:
            print("错误: 没有找到ASP.NET_SessionId Cookie，无法继续。")
            print("请确保您已成功登录网站。")
            QMessageBox.warning(self, "Cookie错误", "没有找到必要的会话Cookie。请确保您已成功登录。")
            return
            
        # 这是我们需要找的sourcetype值
        source_type = hallticket or self.stored_cookies.get("sourcetypeticket")
        if not source_type:
            print("警告: 没有找到sourcetype值 (hallticket或sourcetypeticket)。")
            print("将使用默认值，但查询可能失败。")
            source_type = "2B5553C3C44E4B78BD25AEC09D8358D1"  # 默认值
        
        # 构建cookies字典
        cookies_for_requests = {
            'ASP.NET_SessionId': session_id,
            'imeiticket': self.stored_cookies.get('imeiticket', DEFAULT_IMEI),
            'sourcetypeticket': source_type
        }
        
        self.current_session_id = session_id
        self.current_source_type = source_type
        
        print(f"\n找到的关键Cookie值:")
        print(f"  ASP.NET_SessionId: {session_id}")
        print(f"  sourcetype值: {source_type}")
        
        # 如果没有账号，请求用户通过对话框输入
        if not self.current_account:
            account, ok = QInputDialog.getText(
                self, 
                "输入学号", 
                "请输入您的学号用于查询:", 
                QLineEdit.Normal
            )
            if ok and account:
                self.current_account = account
            else:
                QMessageBox.warning(self, "输入取消", "未输入学号，无法继续。请重试。")
                return
        
        # 设置学号显示
        self.account_input.setText(f"<b>{self.current_account}</b>") # Use bold text
        self.status_label.setText("状态: 正在尝试获取第一页数据...")
        self.status_label.setStyleSheet("color: orange;") # Indicate activity
        QApplication.processEvents() # Update UI immediately
        
        print("\n尝试获取交易流水测试页...")
        success, data = fetch_transactions(
            cookies_for_requests, 
            page=1, 
            source_type=source_type, 
            account=self.current_account, 
            parent_window=self
        )
        
        if success:
            print("\n交易流水测试页获取成功!")
            # 启用获取数据按钮
            self.fetch_data_button.setEnabled(True)
            self.status_label.setText("状态: 准备就绪，可以获取数据")
            self.status_label.setStyleSheet("color: green;") # Indicate success
            
            # 计算总页数并更新UI
            if data and 'total' in data:
                total_records = int(data['total'])
                records_per_page = len(data['rows']) if 'rows' in data else 15
                if records_per_page > 0:
                    total_pages = math.ceil(total_records / records_per_page)
                    self.end_page_input.setMaximum(total_pages)
                    self.end_page_input.setSpecialValueText(f"所有 ({total_pages}页)")
                    self.status_label.setText(f"状态: 就绪，共 {total_records} 条记录，{total_pages} 页")
                    self.status_label.setStyleSheet("color: green;")
            
            # 存储第一页数据
            if 'rows' in data:
                self.all_transaction_records = data['rows']
                self.records_count_label.setText(f"已获取 {len(self.all_transaction_records)} 条记录")
                self.save_button.setEnabled(True)
        else:
            print("\n无法获取交易流水，请检查登录状态和Cookie值。")
            print("\n尝试另一种请求格式...")
            # 尝试使用不同的参数格式
            alternative_cookies = {
                'ASP.NET_SessionId': session_id,
                'imeiticket': self.stored_cookies.get('imeiticket', DEFAULT_IMEI),
                'hallticket': source_type  # 使用hallticket而不是sourcetypeticket
            }
            success, data = fetch_transactions(
                alternative_cookies, 
                page=1, 
                source_type=source_type, 
                account=self.current_account, 
                parent_window=self
            )
            
            if success:
                print("\n使用替代格式成功获取交易流水!")
                self.cookies_for_requests = alternative_cookies  # 保存有效的cookie格式
                
                # 启用获取数据按钮
                self.fetch_data_button.setEnabled(True)
                self.status_label.setText("状态: 准备就绪，可以获取数据")
                self.status_label.setStyleSheet("color: green;")
                
                # 计算总页数并更新UI
                if data and 'total' in data:
                    total_records = int(data['total'])
                    records_per_page = len(data['rows']) if 'rows' in data else 15
                    if records_per_page > 0:
                        total_pages = math.ceil(total_records / records_per_page)
                        self.end_page_input.setMaximum(total_pages)
                        self.end_page_input.setSpecialValueText(f"所有 ({total_pages}页)")
                        self.status_label.setText(f"状态: 就绪，共 {total_records} 条记录，{total_pages} 页")
                        self.status_label.setStyleSheet("color: green;")
                
                # 存储第一页数据
                if 'rows' in data:
                    self.all_transaction_records = data['rows']
                    self.records_count_label.setText(f"已获取 {len(self.all_transaction_records)} 条记录")
                    self.save_button.setEnabled(True)
            else:
                QMessageBox.warning(self, "获取失败", "无法获取交易记录。请确保您已正确登录。")
                self.status_label.setText("状态: 获取测试页失败")
                self.status_label.setStyleSheet("color: red;") # Indicate failure
        
        print("\n已存储的所有 Cookies:")
        for name, value in self.stored_cookies.items():
            print(f"  {name}: {value}")
        print("-" * 20)

    @Slot()
    def start_data_fetch(self):
        """开始获取多页数据"""
        if not self.current_session_id or not self.current_source_type:
            QMessageBox.warning(self, "参数错误", "缺少必要的会话信息。请先提取Cookie。")
            return
        
        if not self.current_account:
            account, ok = QInputDialog.getText(
                self, 
                "输入学号", 
                "请输入您的学号用于查询:", 
                QLineEdit.Normal
            )
            if ok and account:
                self.current_account = account
                self.account_input.setText(self.current_account)
            else:
                QMessageBox.warning(self, "输入取消", "未输入学号，无法继续。")
                return
            
        start_page = self.start_page_input.value()
        end_page = self.end_page_input.value()
        
        # 构建cookies字典
        cookies_for_requests = {
            'ASP.NET_SessionId': self.current_session_id,
            'imeiticket': self.stored_cookies.get('imeiticket', DEFAULT_IMEI),
            'sourcetypeticket': self.current_source_type
        }
        
        # 禁用相关按钮
        self.fetch_data_button.setEnabled(False)
        self.fetch_button.setEnabled(False)
        self.stop_fetch_button.setEnabled(True)
        
        # 重置进度条
        self.progress_bar.setValue(0)
        
        # 创建并启动线程
        self.fetch_thread = DataFetchThread(
            cookies_for_requests,
            self.current_source_type,
            self.current_account,
            start_page,
            end_page
        )
        self.fetch_thread.progress_update.connect(self.update_progress)
        self.fetch_thread.data_received.connect(self.process_data)
        self.fetch_thread.fetch_complete.connect(self.fetch_completed)
        self.fetch_thread.start()
        
        self.status_label.setText(f"状态: 正在获取数据 ({start_page} - {end_page if end_page > 0 else '所有'}页)")
        self.status_label.setStyleSheet("color: blue;") # Indicate ongoing process

    @Slot()
    def stop_data_fetch(self):
        """停止数据获取线程"""
        if self.fetch_thread and self.fetch_thread.isRunning():
            self.fetch_thread.stop()
            self.status_label.setText("状态: 正在停止...")
            self.stop_fetch_button.setEnabled(False)

    @Slot(int, int)
    def update_progress(self, current_page, total_pages):
        """更新进度条"""
        if total_pages > 0:
            progress = int((current_page / total_pages) * 100)
            self.progress_bar.setValue(progress)
            self.status_label.setText(f"状态: 正在获取数据 ({current_page}/{total_pages}页)")
            self.status_label.setStyleSheet("color: blue;")

    @Slot(list)
    def process_data(self, records):
        """处理接收到的数据"""
        if records:
            self.all_transaction_records.extend(records)
            self.records_count_label.setText(f"已获取 {len(self.all_transaction_records)} 条记录")

    @Slot(bool, str)
    def fetch_completed(self, success, message):
        """数据获取完成的处理"""
        # 恢复按钮状态
        self.fetch_data_button.setEnabled(True)
        self.fetch_button.setEnabled(True)
        self.stop_fetch_button.setEnabled(False)
        
        if success:
            self.status_label.setText(f"状态: {message}")
            self.save_button.setEnabled(len(self.all_transaction_records) > 0)
            QMessageBox.information(self, "获取完成", message)
            self.status_label.setStyleSheet("color: green;")
        else:
            self.status_label.setText(f"状态: 失败 - {message}")
            QMessageBox.warning(self, "获取失败", message)
            self.status_label.setStyleSheet("color: red;")

    @Slot()
    def save_data(self):
        """保存数据到文件"""
        if not self.all_transaction_records:
            QMessageBox.warning(self, "保存失败", "没有可保存的数据。")
            return
            
        # 获取当前时间作为文件名一部分
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        default_filename = f"交易记录_{self.current_account or ''}_{timestamp}"
        
        # 根据选择的格式确定文件类型
        file_format = self.save_format_combo.currentText()
        if file_format == "CSV":
            filename, _ = QFileDialog.getSaveFileName(
                self, "保存交易记录", 
                default_filename + ".csv", 
                "CSV Files (*.csv);;All Files (*)"
            )
            if filename:
                if save_transactions_to_csv(self.all_transaction_records, filename):
                    QMessageBox.information(self, "保存成功", f"成功保存 {len(self.all_transaction_records)} 条记录到文件: {filename}")
                else:
                    QMessageBox.warning(self, "保存失败", "保存文件时发生错误。")
        else:  # JSON
            filename, _ = QFileDialog.getSaveFileName(
                self, "保存交易记录", 
                default_filename + ".json", 
                "JSON Files (*.json);;All Files (*)"
            )
            if filename:
                if save_transactions_to_json(self.all_transaction_records, filename):
                    QMessageBox.information(self, "保存成功", f"成功保存 {len(self.all_transaction_records)} 条记录到文件: {filename}")
                else:
                    QMessageBox.warning(self, "保存失败", "保存文件时发生错误。")


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = LoginWindow()
    window.show()
    sys.exit(app.exec()) 