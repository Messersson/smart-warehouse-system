# 企业化基线说明

本项目已具备以下企业级基础能力：

- 后端 API 统一 token 鉴权，除登录与健康检查外，`/api/**` 默认需要登录。
- 后端按角色菜单路径做接口级 RBAC 校验，避免仅依赖前端隐藏菜单。
- 登录 token 使用 HMAC-SHA256 签名并带过期时间。
- 用户密码使用 PBKDF2 哈希保存，兼容历史明文密码并在登录或编辑时自动升级。
- 核心写入 DTO 增加服务端参数校验，非法请求返回统一业务响应。
- 每个请求自动生成或透传 `X-Request-Id`，日志中带 requestId，便于排查生产问题。
- CORS、token 密钥、token 过期时间、数据库密码支持环境变量配置。
- 前端请求统一注入 `Authorization: Bearer <token>`。

## 生产环境必配变量

```bash
MYSQL_ROOT_PASSWORD=请使用强密码
WMS_TOKEN_SECRET=请使用至少32位随机密钥
WMS_TOKEN_EXPIRATION_MINUTES=720
WMS_CORS_ALLOWED_ORIGINS=https://your-domain.example.com
```

## 建议继续完善

- 接入 Spring Security 或网关层鉴权，支持更细粒度的接口权限码。
- 引入 Flyway/Liquibase 管理数据库版本迁移。
- 增加单元测试、接口集成测试和 CI/CD 流水线。
- 将日志输出改为 JSON，接入 ELK、Loki 或云日志平台。
- 对关键业务操作补充审计表，例如库存调整、角色授权、账单付款。
