package com.tthih.yu.about

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class OpenSourceLicenseActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_source_license)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "开源许可"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 设置WebView加载开源许可内容
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = false
        
        // 加载开源许可内容
        loadOpenSourceLicenseContent()
    }
    
    private fun loadOpenSourceLicenseContent() {
        // 使用HTML字符串直接加载内容
        val licenseContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #333; }
                    h1 { font-size: 20px; color: #000; }
                    h2 { font-size: 18px; color: #000; margin-top: 24px; }
                    h3 { font-size: 16px; color: #000; margin-top: 16px; }
                    p { margin-bottom: 16px; }
                    pre { background-color: #f5f5f5; padding: 10px; overflow: auto; font-size: 12px; }
                </style>
            </head>
            <body>
                <h1>开源许可</h1>
                <p>YU校园助手使用了以下开源项目，在此表示感谢。</p>
                
                <h2>Apache License 2.0</h2>
                
                <h3>Retrofit</h3>
                <p>A type-safe HTTP client for Android and Java.</p>
                <p>Copyright (C) 2013 Square, Inc.</p>
                <p><a href="https://github.com/square/retrofit">https://github.com/square/retrofit</a></p>
                
                <h3>OkHttp</h3>
                <p>An HTTP client for Android, Kotlin, and Java.</p>
                <p>Copyright (C) 2019 Square, Inc.</p>
                <p><a href="https://github.com/square/okhttp">https://github.com/square/okhttp</a></p>
                
                <h3>Gson</h3>
                <p>A Java serialization/deserialization library to convert Java Objects into JSON and back.</p>
                <p>Copyright (C) 2008 Google Inc.</p>
                <p><a href="https://github.com/google/gson">https://github.com/google/gson</a></p>
                
                <h3>Glide</h3>
                <p>An image loading and caching library for Android focused on smooth scrolling.</p>
                <p>Copyright (C) 2014 Google, Inc.</p>
                <p><a href="https://github.com/bumptech/glide">https://github.com/bumptech/glide</a></p>
                
                <h2>MIT License</h2>
                
                <h3>Lottie for Android</h3>
                <p>Render After Effects animations natively on Android and iOS, Web, and React Native.</p>
                <p>Copyright (C) 2018 Airbnb, Inc.</p>
                <p><a href="https://github.com/airbnb/lottie-android">https://github.com/airbnb/lottie-android</a></p>
                
                <h3>Material Calendar View</h3>
                <p>A Material design back port of Android's CalendarView.</p>
                <p>Copyright (c) 2016 Prolific Interactive</p>
                <p><a href="https://github.com/prolificinteractive/material-calendarview">https://github.com/prolificinteractive/material-calendarview</a></p>
                
                <h2>BSD License</h2>
                
                <h3>EventBus</h3>
                <p>Android optimized event bus that simplifies communication between Activities, Fragments, Threads, Services, etc.</p>
                <p>Copyright (C) 2012-2020 Markus Junginger, greenrobot.</p>
                <p><a href="https://github.com/greenrobot/EventBus">https://github.com/greenrobot/EventBus</a></p>
                
                <h2>License Details</h2>
                
                <h3>Apache License 2.0</h3>
                <pre>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
                </pre>
                
                <h3>MIT License</h3>
                <pre>
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
                </pre>
                
                <h3>BSD License</h3>
                <pre>
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
* Neither the name of greenrobot nor the
  names of its contributors may be used to endorse or promote products
  derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL GREENROBOT BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                </pre>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, licenseContent, "text/html", "UTF-8", null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 