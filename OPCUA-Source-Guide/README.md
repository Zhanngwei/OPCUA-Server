# OPCUA-Server 源码学习指南

这套文档从当前仓库源码出发，帮助不熟悉 OPC UA 的 Java/Web 开发者同时理解协议、项目架构和实际操作。

## 阅读入口

- [Markdown 完整版](./OPCUA源码学习指南.md)：适合在 IDE、GitHub 或 Gitee 中边看文档边跳转源码；架构图使用 Mermaid。
- [HTML 离线版](./OPCUA源码学习指南.html)：双击即可阅读，包含响应式目录、内嵌样式、脚本和 SVG 图，不依赖网络资源。
- [设计说明](./specs/2026-07-20-opcua-source-guide-design.md)：文档范围、内容原则和验收标准。
- [实施计划](./plans/2026-07-20-opcua-source-guide.md)：源码取证、文档生成和验证步骤。

## 三条学习路线

### 30 分钟了解

阅读完整指南第 1～7 章和第 15 章，掌握：

- OPC UA 信息模型、通信模型和安全模型。
- 本项目的双入口架构及核心类职责。
- 标准能力、Milo 能力和项目实现之间的边界。

### 半天快速上手

在前一条路线基础上完成第 10～13 章：

- 启动 Spring Boot 与 Milo Server。
- 使用 UaExpert 浏览、读取和订阅 HelloWorld。
- 使用 Web UI 创建、修改和删除动态节点。
- 使用 REST API 登录、单点/批量建点。
- 验证 H2 节点恢复和客户端证书信任流程。

### 二次开发

重点阅读第 8、9、14、16、18 章：

- 理解 `DynamicNodeManager` 和 `NodePersistenceService`。
- 将真实设备数据映射为 UaVariableNode。
- 识别端点配置、证书目录、默认账号、公开写接口等生产风险。
- 从简单点表逐步扩展到 OPC UA 类型模型。

## 文档依据与限制

- 内容基于 2026-07-20 的 `D:\projectSelf\OPCUA-Server` 工作区源码。
- 源码文件移动或代码增删后，指南中的行号可能变化，应以类名、方法名和最新源码为准。
- 本次生成环境没有可用的 Maven 命令，未实际运行 Java 测试或启动服务器；实战步骤根据源码、配置和现有项目说明整理，并明确标注了该限制。
- 指南用于源码学习和项目上手，不替代 OPC Foundation 官方规范、合规要求或安全评估。

## 最短启动命令

环境具备 JDK 17 和 Maven 3.8+ 后：

```powershell
cd D:\projectSelf\OPCUA-Server
mvn spring-boot:run -Pskip-frontend
```

默认入口：

- Web：`http://localhost:8166`
- OPC UA：`opc.tcp://localhost:4840/milo`
- Discovery：`opc.tcp://localhost:4840/milo/discovery`
