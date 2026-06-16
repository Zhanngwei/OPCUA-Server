# OPC UA 服务器管理前端

这是一个基于 Vue 3 + Element Plus 的 OPC UA 服务器管理界面，提供类似专业 OPC UA 客户端的功能。

## 🚀 功能特性

### 📁 节点管理
- **树形结构显示** - 左侧面板显示 OPC UA 节点的层次结构
- **文件夹管理** - 创建、删除文件夹节点
- **变量管理** - 创建、删除、更新变量节点
- **搜索功能** - 快速搜索节点

### 📊 属性面板
- **Attributes 标签页** - 显示和编辑节点属性
- **References 标签页** - 显示节点引用关系
- **Events 标签页** - 监控节点事件

### 🔧 工具栏
- **添加文件夹** - 创建新的文件夹节点
- **添加变量** - 创建新的变量节点
- **删除节点** - 删除选中的节点
- **刷新** - 刷新节点树

### 🎛️ 高级功能
- **权限控制** - 支持 READ_WRITE、READ_ONLY、WRITE_ONLY、NONE 权限
- **数据类型** - 支持 Double、Integer、String、Boolean 等数据类型
- **实时监控** - 事件监控和值变化追踪
- **命名空间** - 支持多命名空间管理

## 🛠️ 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - Vue 3 组件库
- **Pinia** - Vue 状态管理
- **Axios** - HTTP 客户端
- **Vue Router** - 路由管理
- **ECharts** - 图表库（预留）

## 📦 安装和运行

### 前置要求
- Node.js 16+ 
- npm 或 yarn

### 安装依赖
```bash
cd frontend
npm install
```

### 开发模式运行
```bash
npm run serve
```
访问 http://localhost:3000

### 生产构建
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

## 🔗 API 接口

前端通过以下 API 与后端通信：

### 节点管理
- `GET /api/nodes` - 获取所有节点
- `POST /api/nodes/folder` - 创建文件夹
- `POST /api/nodes/variable` - 创建变量
- `PUT /api/nodes/variable/{path}` - 更新变量值
- `DELETE /api/nodes/{path}` - 删除节点
- `GET /api/nodes/{path}/details` - 获取节点详情

### 请求示例

#### 创建文件夹
```json
POST /api/nodes/folder
{
  "path": "devices/sensors",
  "displayName": "传感器设备"
}
```

#### 创建变量
```json
POST /api/nodes/variable
{
  "path": "devices/sensors/temperature",
  "displayName": "温度传感器",
  "dataType": "double",
  "initialValue": 25.5,
  "accessLevel": "READ_WRITE"
}
```

#### 更新变量值
```json
PUT /api/nodes/variable/devices/sensors/temperature
{
  "value": 26.8
}
```

## 🎨 界面说明

### 主界面布局
```
┌─────────────────────────────────────────────────────────┐
│ 工具栏: [+] [-] [🔄] [✏️]                Objects        │
├─────────────────┬───────────────────────────────────────┤
│                 │ 📑 Attributes │ 🔗 References │ 📅 Events │
│   🔍 搜索框      ├───────────────────────────────────────┤
│                 │                                       │
│ 📁 Objects      │        属性表格/引用列表/事件监控        │
│  ├─ 📁 devices  │                                       │
│  │  ├─ 📊 temp  │                                       │
│  │  └─ 📊 humi  │                                       │
│  └─ 📁 system   │                                       │
│                 │                                       │
└─────────────────┴───────────────────────────────────────┘
```

### 对话框
- **添加文件夹对话框** - 设置文件夹参数
- **添加变量对话框** - 设置变量参数、数据类型、权限等

## 🔧 配置说明

### 代理配置
开发环境下，前端会自动代理 API 请求到后端服务器：
```javascript
// vue.config.js
devServer: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 环境变量
可以通过环境变量配置后端 API 地址：
```bash
VUE_APP_API_BASE_URL=http://localhost:8080/api
```

## 🐛 故障排除

### 常见问题

1. **无法连接后端**
   - 确保后端服务器在 http://localhost:8080 运行
   - 检查防火墙设置

2. **节点创建失败**
   - 检查节点路径是否重复
   - 确认数据类型和初始值匹配

3. **权限错误**
   - 检查节点的访问权限设置
   - 确认用户权限级别

### 调试模式
在浏览器开发者工具中查看网络请求和控制台日志。

## 📝 开发说明

### 项目结构
```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/          # 组件
│   │   ├── AttributesPanel.vue
│   │   ├── ReferencesPanel.vue
│   │   ├── EventsPanel.vue
│   │   ├── AddFolderDialog.vue
│   │   └── AddVariableDialog.vue
│   ├── stores/             # 状态管理
│   │   └── opcua.js
│   ├── styles/             # 样式
│   │   └── global.css
│   ├── views/              # 页面
│   │   └── OpcuaManager.vue
│   ├── router/             # 路由
│   │   └── index.js
│   ├── App.vue
│   └── main.js
├── package.json
├── vue.config.js
└── README.md
```

### 添加新功能
1. 在 `components/` 目录下创建新组件
2. 在 `stores/opcua.js` 中添加相应的状态管理
3. 在后端添加对应的 API 接口
4. 更新路由和主界面

## 📄 许可证

MIT License
