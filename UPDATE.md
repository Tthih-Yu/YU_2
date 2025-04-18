# YU校园助手应用更新发布指南

本文档详细说明了如何为YU校园助手应用准备、发布新版本以及配置服务器端更新系统。

## 目录

1. [版本号规范](#版本号规范)
2. [APK打包步骤](#APK打包步骤)
3. [服务器配置](#服务器配置)
4. [发布新版本流程](#发布新版本流程)
5. [强制更新说明](#强制更新说明)
6. [常见问题排查](#常见问题排查)

## 版本号规范

YU校园助手使用以下版本号规范：

- **versionName**: 采用 `x.y.z` 格式，如 `1.2.0`
  - x: 主版本号，有重大功能变更时增加
  - y: 次版本号，有新功能添加时增加
  - z: 修订版本号，bug修复时增加

- **versionCode**: 整数值，每次发布必须增加
  - 计算方式: `x*100 + y*10 + z`
  - 例如：版本1.2.3的versionCode为123

## APK打包步骤

1. **更新应用版本**
   - 打开 `app/build.gradle` 文件
   - 修改 `versionCode` 和 `versionName`
   ```gradle
   android {
       defaultConfig {
           versionCode 110 // 更新为新的版本码
           versionName "1.1.0" // 更新为新的版本名
       }
   }
   ```

2. **构建签名APK**
   - 在Android Studio中：Build > Generate Signed Bundle / APK
   - 选择APK，然后选择签名密钥库
   - 选择release构建变体
   - 完成构建过程

3. **重命名APK文件**
   - 将生成的APK文件重命名为标准格式：`YU_版本号.apk`
   - 例如：`YU_1.1.0.apk`

## 服务器配置

服务器使用Node.js Express应用提供更新API，配置在腾讯云服务器上。

### 基本信息

- **服务器IP**: 43.143.7.45
- **端口**: 3000
- **更新API路径**: http://43.143.7.45:3000/api/update
- **APK存储目录**: /var/www/update-api/downloads/

### 安全配置

1. **确保安全组规则允许3000端口**：
   - 登录腾讯云控制台
   - 导航到云服务器安全组设置
   - 添加入站规则：TCP协议，端口3000，源IP：0.0.0.0/0

### API响应格式

更新API应返回以下JSON格式：

```json
{
    "hasUpdate": true,
    "data": {
        "versionName": "1.1.0",
        "versionCode": 110,
        "forceUpdate": false,
        "updateContent": "1. 新增功能A\n2. 修复Bug B\n3. 优化性能C",
        "downloadUrl": "http://43.143.7.45:3000/downloads/YU_1.1.0.apk",
        "publishDate": "2023-04-14"
    }
}
```

## 发布新版本流程

1. **构建和测试APK**
   - 按照[APK打包步骤](#APK打包步骤)生成签名APK
   - 在至少2-3台不同设备上测试APK

2. **上传APK到服务器**
   ```bash
   scp YU_版本号.apk username@43.143.7.45:/var/www/update-api/downloads/
   ```

3. **更新服务器配置**
   ```bash
   # SSH连接到服务器
   ssh username@43.143.7.45
   
   # 进入应用目录
   cd /var/www/update-api
   
   # 编辑更新配置
   nano app.js
   ```

4. **修改版本信息**
   在app.js中更新以下内容：
   ```javascript
   // 更新版本号
   const latestVersion = 110; // 修改为新版本的versionCode
   
   // 更新响应数据
   res.json({
       hasUpdate: true,
       data: {
           versionName: "1.1.0", // 修改为新版本名称
           versionCode: 110,     // 修改为新版本号
           forceUpdate: false,   // 是否强制更新
           updateContent: "1. 新功能A\n2. 修复Bug B\n3. 优化性能C", // 更新内容
           downloadUrl: "http://43.143.7.45:3000/downloads/YU_1.1.0.apk", // 更新下载路径
           publishDate: "2023-04-14" // 发布日期
       }
   });
   ```

5. **重启更新服务**
   ```bash
   # 重启Node.js应用
   pm2 restart update-api
   ```

6. **验证更新API**
   在浏览器中访问：
   ```
   http://43.143.7.45:3000/api/update?version=100
   ```
   确认返回正确的更新信息

7. **验证下载链接**
   在浏览器中访问APK下载链接，确认能够下载APK文件：
   ```
   http://43.143.7.45:3000/downloads/YU_版本号.apk
   ```

## 强制更新说明

当应用有重大变更或安全修复时，可以使用强制更新功能：

1. 在app.js中将`forceUpdate`设置为`true`：
   ```javascript
   data: {
       // 其他字段...
       forceUpdate: true,
       // 其他字段...
   }
   ```

2. 强制更新模式下，用户必须更新到最新版本才能继续使用应用。

## 常见问题排查

### 更新API访问失败

1. **检查服务器状态**
   ```bash
   # 查看应用运行状态
   pm2 status
   
   # 查看应用日志
   pm2 logs update-api
   ```

2. **检查防火墙设置**
   ```bash
   # 查看防火墙状态
   sudo ufw status
   
   # 检查端口是否监听
   sudo netstat -tulpn | grep 3000
   ```

3. **确认Node.js绑定地址**
   确保app.js中的监听设置为：
   ```javascript
   app.listen(port, '0.0.0.0', () => {
     console.log(`更新服务器运行在 http://0.0.0.0:${port}`);
   });
   ```

### APK下载失败

1. **检查文件权限**
   ```bash
   # 检查文件权限
   ls -la /var/www/update-api/downloads/
   
   # 修正权限（如果需要）
   sudo chmod 644 /var/www/update-api/downloads/YU_版本号.apk
   ```

2. **验证静态文件服务配置**
   确保app.js中的static配置正确：
   ```javascript
   app.use('/downloads', express.static('/var/www/update-api/downloads'));
   ```

### 应用无法安装APK

1. **检查APK签名**：确保使用了正确的签名密钥
2. **检查Android版本兼容性**：确保APK兼容用户的Android版本
3. **验证APK完整性**：确保下载的APK文件未损坏

---

**注意**：每次发布新版本后，建议保留旧版本APK一段时间，以便用户可以在必要时回滚。

**最后更新日期**：2023年04月14日 