# 仓库系统

基于以下技术栈搭建的仓库系统基础版：

- 后端：Java 17 + Spring Boot
- 前端：Vue 2.7 + Element UI + Vite
- 数据库：MySQL 8
- 环境：Docker / Docker Compose

项目目标：

- 提供仓库、库位、货主、供应商、客户、商品等主数据管理
- 提供入库、出库、库存、库存流水、预警中心、驾驶舱等核心能力
- 提供滞留预警、出库超时预警、呆滞库存预警等自动扫描能力
- 提供一份可直接导入 MySQL 的单文件 SQL 脚本

## 目录结构

```text
backend/                 Spring Boot 后端
frontend/                Vue + Element UI 前端
deploy/mysql/            MySQL 初始化脚本
docker-compose.yml       Docker 编排
```

## 数据库初始化

数据库脚本放在 `deploy/mysql/warehouse_system.sql`

初始化后默认库名：`warehouse_system`

示例登录账号：

- 用户名：`admin`
- 密码：`admin123`

## 本地运行

### 1. 先导入 MySQL

导入 `deploy/mysql/warehouse_system.sql`

### 2. 启动后端

进入 `backend`

```powershell
mvn spring-boot:run
```

默认端口：`8080`

如果本机没有 Maven，建议直接使用 Docker 启动，Docker 构建会在容器内完成 Maven 打包。

### 3. 启动前端

进入 `frontend`

```powershell
npm install
npm run dev
```

默认端口：`5173`

如果 PowerShell 提示 `npm.ps1` 执行受限，可改用：

```powershell
npm.cmd install
npm.cmd run dev
```

## Docker 启动

如果你本机安装了 Docker，可直接在项目根目录执行：

```powershell
docker compose up --build
```

也可以使用项目自带启动器：

```powershell
.\start-project.bat
```

如果需要强制重新构建镜像：

```powershell
.\start-project.bat -Build
```

如果希望启动完成后继续跟随日志：

```powershell
.\start-project.bat -FollowLogs
```

启动器会自动：

- 检查 Docker / Docker Compose 是否可用
- 启动 `mysql`、`backend`、`frontend`
- 轮询检查各部分启动状态
- 打印 MySQL、后端、前端的最终启动结果
- 启动失败时打印对应服务最近日志

容器默认端口：

- 前端：`80`
- 后端：`18080`
- MySQL：`3307`

## 已实现模块

- 登录
- 首页驾驶舱
- 仓库管理
- 库位管理
- 货主管理
- 供应商管理
- 客户管理
- 商品管理
- 入库单管理
- 出库单管理
- 实时库存查询
- 预警中心
- 定时扫描滞留与超时任务
- 用户权限与菜单权限
- 盘点模块
- 计费结算模块
- 订单审批与异常工单模块

## 说明

这是一个可运行、可继续扩展的商业仓库系统骨架，已经把数据库模型、核心业务链路和前后台界面串起来。更复杂的计费、审批流、客户门户、财务对账、设备集成、WCS/AGV 对接等能力已经在 SQL 模型中预留了扩展表结构，可在此基础上继续开发。
