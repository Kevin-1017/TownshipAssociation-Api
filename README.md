# tsa-api · 乡会项目后端

乡会平台的统一后端服务：同时为 **uni-app 微信小程序** 与未来的 **React 官网** 提供 RESTful API。

- 技术选型总说明：[docs/TECH_STACK.md](docs/TECH_STACK.md)（为什么是这套栈、明确不用什么）
- **开发规范（必读）**：[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)（Git 流程、分层纪律、PR 自查清单）
- 数据库设计：[docs/DATABASE.md](docs/DATABASE.md)
- 接口规范：[docs/API.md](docs/API.md)
- 在线接口文档（启动后）：http://localhost:8080/doc.html

## 技术栈速览

Spring Boot 3.5 · JDK 25 · MyBatis-Plus · MySQL 8 · Sa-Token · Knife4j · Maven Wrapper

## 5 分钟跑起来

前置：JDK 17+（推荐 25，位置 `D:/JDK/jdk-25`）、MySQL 8 服务（本机服务名 `MySQL80`）。**无需安装 Maven**（仓库自带 Wrapper）。

```bash
# 1. 启动 MySQL（Windows 管理员终端）
net start MySQL80

# 2. 建库建表（含演示数据）
mysql -uroot -p < sql/schema.sql

# 3. 填入你本机的 MySQL root 密码
#    编辑 src/main/resources/application-dev.yml 中的 spring.datasource.password

# 4. 启动项目（首次运行会自动下载 Maven 与依赖）
./mvnw.cmd spring-boot:run        # Windows
```

看到 `Tomcat started on port 8080` 即成功，验证：

```bash
curl http://localhost:8080/tsa/health
curl "http://localhost:8080/tsa/members?page=1&size=5"
curl http://localhost:8080/tsa/members/map-data
```

## 常用命令

| 命令 | 作用 |
|------|------|
| `./mvnw.cmd clean test` | 编译 + 跑测试（不需要数据库） |
| `./mvnw.cmd spring-boot:run` | 启动开发服务器 |
| `./mvnw.cmd clean package` | 打包可执行 jar 到 `target/` |

## 给新同学的路线

1. 读 [docs/TECH_STACK.md](docs/TECH_STACK.md) —— 理解这套项目"为什么长这样"
2. 从 `controller → service → mapper → entity` 顺序读一遍 `MemberController` 这条线
3. 跑 `MemberControllerTest`，看懂不连数据库怎么测接口
4. 认领一个 L1 任务练手（见 TECH_STACK §4 任务分级）

## 目录结构

```
src/main/java/com/tsa/api/
├── common/     统一响应 Result、全局异常处理
├── config/     分页/填充、Sa-Token 鉴权、跨域、文档
├── controller/ HTTP 接口层（L1 学生从这里开始）
├── service/    业务逻辑层（L2~L3）
├── mapper/     数据访问层（MyBatis-Plus）
├── entity/     数据库表实体
└── dto/        请求/响应传输对象（校验注解在这里）
```
