# Maven 依赖刷新指南

## 问题描述
添加 `langchain4j-document-parser-apache-tika` 依赖后，IDE 可能仍报错"无法解析符号"。

## 解决方案

### 方法1：Maven Reload（推荐）
**IntelliJ IDEA：**
1. 右侧 Maven 面板 → 点击 🔄 "Reload All Maven Projects"
2. 或使用快捷键：`Ctrl + Shift + O`（Windows/Linux）或 `Cmd + Shift + I`（Mac）

**Eclipse：**
1. 右键项目 → Maven → Update Project
2. 勾选 "Force Update of Snapshots/Releases"
3. 点击 OK

### 方法2：命令行刷新
```bash
cd E:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge
mvn clean install -DskipTests
```

### 方法3：清除 IDE 缓存
**IntelliJ IDEA：**
1. File → Invalidate Caches / Restart
2. 选择 "Invalidate and Restart"

## 验证步骤
刷新完成后，检查以下类是否不再报错：
- [ ] `ApacheTikaDocumentParser`
- [ ] `FileStorageService.parse()` 方法

## 如果仍然报错
1. 检查网络连接（需要从 sonatype 仓库下载）
2. 查看 Maven Local Repository：
   ```bash
   ls ~/.m2/repository/dev/langchain4j/langchain4j-document-parser-apache-tika/
   ```
3. 手动下载依赖：
   ```bash
   mvn dependency:resolve
   ```

## 临时方案（仅用于测试）
如果暂时无法刷新依赖，可以回退到原有的手动解析方式，但**强烈建议尽快迁移到 Tika**。
