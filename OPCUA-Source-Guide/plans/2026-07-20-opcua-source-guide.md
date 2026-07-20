# OPC UA Source Learning Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成一套以当前仓库源码为依据、覆盖 OPC UA 入门知识、项目架构、核心功能和可复现实战的 Markdown 与离线 HTML 中文指南。

**Architecture:** 以 `OPCUA源码学习指南.md` 作为内容基线，协议概念和项目实现采用同一章节中的双向映射；`OPCUA源码学习指南.html` 提供等价的离线阅读体验，图形使用内嵌 SVG/HTML，避免网络依赖。`README.md` 只承担入口、推荐路线和验证说明，避免复制正文。

**Tech Stack:** Markdown、Mermaid、HTML5、CSS3、原生 JavaScript、PowerShell、Java 17、Spring Boot 3、Eclipse Milo 1.0.0、Vue 3、H2。

---

## 文件结构

- Create: `OPCUA-Source-Guide/README.md` — 阅读入口、交付物说明、推荐学习路线。
- Create: `OPCUA-Source-Guide/OPCUA源码学习指南.md` — 协议、源码、架构与实战的完整内容基线。
- Create: `OPCUA-Source-Guide/OPCUA源码学习指南.html` — 单文件离线版，包含目录、样式、脚本和内嵌图示。
- Existing: `OPCUA-Source-Guide/specs/2026-07-20-opcua-source-guide-design.md` — 已确认的范围和验收标准。
- Existing: `OPCUA-Source-Guide/plans/2026-07-20-opcua-source-guide.md` — 本实施计划。

### Task 1: 建立源码事实清单

**Files:**
- Inspect: `pom.xml`
- Inspect: `endpoint-config.json`
- Inspect: `src/main/resources/application.properties`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/OpcUaServerApplication.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/SimpleOpcUaServer.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/SimpleNamespace.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/DynamicNodeManager.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/server/OpcUaServerManager.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/controller/*.java`
- Inspect: `src/main/java/org/go/show/multiproto/opcuaserver/service/*.java`
- Inspect: `frontend/src/router/index.js`
- Inspect: `frontend/src/stores/opcua.js`

- [x] **Step 1: 提取关键类、方法、端点和配置的精确行号**

Run:

```powershell
rg -n "class |startup|createEndpointConfigs|createAndAddNodes|loadPersistedNodes|createFolder|createVariable|updateVariable|onDataItemsCreated|@(?:Get|Post|Put|Delete|Request)Mapping" src/main/java
```

Expected: 输出启动、端点、命名空间、节点 CRUD、订阅回调和 REST 映射所在行。

- [x] **Step 2: 核对技术栈和运行时默认值**

Run:

```powershell
rg -n "spring-boot.version|milo.version|maven.compiler|server.port|datasource|TCP_BIND_PORT|DEFAULT_PORT|opc.tcp" pom.xml src/main/resources/application.properties src/main/java endpoint-config.json
```

Expected: Java 17、Spring Boot 3.1.5、Milo 1.0.0、Web 8166、OPC UA 4840 和 H2 文件库均有源码证据。

- [x] **Step 3: 核对项目能力边界**

Run:

```powershell
rg -ni "event|history|historizing|methodnode|pubsub|alarm|condition|registerToLDS|SubscriptionModel|MonitoredItem" src pom.xml endpoint-config.json
```

Expected: 找到数据项订阅实现和 LDS 配置字段；没有完整业务实现的能力在指南中标记为“未见完整实现”，不写成绝对不支持。

### Task 2: 编写 Markdown 完整指南

**Files:**
- Create: `OPCUA-Source-Guide/OPCUA源码学习指南.md`

- [x] **Step 1: 编写阅读地图、协议入门和核心术语**

正文必须包含 Client/Server、地址空间、NodeClass、NodeId、Namespace、Attribute、Reference、Endpoint、SecureChannel、Session、Subscription 和 MonitoredItem，并使用一张 Mermaid 分层图说明关系。

- [x] **Step 2: 编写项目架构和启动链路**

正文必须包含总体架构图、启动时序图、核心类职责表、技术栈表和目录导读，并引用 Task 1 获得的源码路径及行号。

- [x] **Step 3: 编写地址空间、读写订阅、安全和持久化源码导读**

正文必须解释 `SimpleNamespace`、`DynamicNodeManager`、`SimpleOpcUaServer`、`NodePersistenceService`、`CertificateService`、身份验证器和 `StatusService` 的协作关系；至少包含节点数据流图、安全流程图和浏览/读写/订阅时序图。

- [x] **Step 4: 编写三类可执行实战**

正文必须提供 Maven 启动、UaExpert 连接与订阅、Web UI 建点、REST API 登录/建点/改值、重启后持久化验证和证书信任实验。每个实验包含目标、操作、预期现象和排错提示。

- [x] **Step 5: 编写扩展、能力矩阵和生产检查清单**

正文必须区分标准、Milo 和项目责任；列出已有、部分、未见完整实现的能力；说明如何接入真实数据源，并提示默认账号、数据库凭据、None 安全端点、H2 Console 和证书目录风险。

- [x] **Step 6: 校验 Markdown 结构**

Run:

```powershell
$f='OPCUA-Source-Guide/OPCUA源码学习指南.md'; rg -n '^#{1,6} |^```|mermaid|UaExpert|DynamicNodeManager|能力矩阵|生产' $f
```

Expected: 章节完整，代码围栏和 Mermaid 图存在，协议、源码、实战、安全与边界章节均能被定位。

### Task 3: 制作单文件离线 HTML

**Files:**
- Create: `OPCUA-Source-Guide/OPCUA源码学习指南.html`

- [x] **Step 1: 建立语义化页面骨架和响应式目录**

使用 `header`、`nav`、`main`、`section`、`footer`，为每个一级章节分配唯一 `id`；内嵌 CSS 实现桌面侧栏和移动端折叠菜单。

- [x] **Step 2: 将 Markdown 内容等价转换为 HTML**

保留协议解释、源码映射、实战步骤、表格、代码示例、提示框和源码路径。HTML 不引用 CDN、远程字体、外部 CSS 或外部 JavaScript。

- [x] **Step 3: 绘制离线架构与时序图**

至少加入总体架构、启动流程、动态节点数据流、安全信任流程和 OPC UA 服务交互图。使用内嵌 SVG 或语义化 HTML/CSS，并为每幅图提供 `aria-label` 和文字说明。

- [x] **Step 4: 加入轻量导航脚本**

内嵌原生 JavaScript 实现移动端目录开关、目录点击后收起、滚动章节高亮和返回顶部；脚本失败时正文仍可完整阅读。

- [x] **Step 5: 校验 HTML 基本结构和离线性**

Run:

```powershell
$f='OPCUA-Source-Guide/OPCUA源码学习指南.html'; rg -n '<!doctype html>|<html lang="zh-CN">|<nav|<main|<section|<svg|<script>|</html>' $f; rg -n 'https?://|src="[^#d][^"]*"|href="https?://' $f
```

Expected: 第一条能定位完整 HTML 结构、图形和脚本；第二条除正文中的示例 URL 纯文本外，不出现外部资源加载标签。

### Task 4: 编写入口 README

**Files:**
- Create: `OPCUA-Source-Guide/README.md`

- [x] **Step 1: 编写阅读入口和学习路径**

README 必须链接 Markdown、HTML、设计说明和实施计划，并给出“30 分钟了解”“半天上手”“二次开发”三条阅读路线。

- [x] **Step 2: 说明文档版本依据和使用方法**

注明文档基于 2026-07-20 工作区源码、HTML 可离线打开、源码移动后行号可能变化，并说明指南不是 OPC Foundation 规范的替代品。

### Task 5: 全量验证和提交

**Files:**
- Verify: `OPCUA-Source-Guide/README.md`
- Verify: `OPCUA-Source-Guide/OPCUA源码学习指南.md`
- Verify: `OPCUA-Source-Guide/OPCUA源码学习指南.html`

- [x] **Step 1: 验证文件、编码和占位符**

Run:

```powershell
Get-ChildItem OPCUA-Source-Guide -File | Select-Object Name,Length; rg -n "TBD|TODO|待补充|占位符|lorem ipsum" OPCUA-Source-Guide -g "*.md" -g "*.html"
```

Expected: 三个主要交付物均非空；除解释“不得存在 TODO/TBD”的规格文本外无占位内容。

- [x] **Step 2: 验证本地路径引用**

Run:

```powershell
$text=Get-Content -Raw -Encoding utf8 'OPCUA-Source-Guide/OPCUA源码学习指南.md'; [regex]::Matches($text,'`((?:src|frontend)/[^`]+?\.(?:java|js|vue|properties))(?::\d+)?`') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | ForEach-Object { if(-not (Test-Path $_)){ "MISSING: $_" } }
```

Expected: 不输出 `MISSING`。

- [x] **Step 3: 验证 Markdown 围栏、HTML 锚点和外部依赖**

Run:

```powershell
@'
from pathlib import Path
import re
md = Path('OPCUA-Source-Guide/OPCUA源码学习指南.md').read_text(encoding='utf-8')
html = Path('OPCUA-Source-Guide/OPCUA源码学习指南.html').read_text(encoding='utf-8')
assert md.count('```') % 2 == 0, 'Markdown code fences are unbalanced'
ids = set(re.findall(r'\bid="([^"]+)"', html))
anchors = set(re.findall(r'href="#([^"]+)"', html))
missing = sorted(anchors - ids)
assert not missing, f'Missing anchor targets: {missing}'
assert not re.search(r'<(?:script|link|img)[^>]+(?:src|href)="https?://', html, re.I), 'External runtime dependency found'
assert all(term in md and term in html for term in ['NodeId','SecureChannel','DynamicNodeManager','UaExpert','能力矩阵'])
print(f'PASS: fences={md.count("```")}, ids={len(ids)}, anchors={len(anchors)}')
'@ | python -
```

Expected: 输出 `PASS` 且进程退出码为 0。

- [x] **Step 4: 检查差异并提交**

Run:

```powershell
git diff --check; git status --short
```

Expected: `git diff --check` 无错误，状态仅包含本指南计划内文件。

Commit:

```powershell
git add OPCUA-Source-Guide
git commit -m "docs: add OPC UA protocol and source guide"
```
