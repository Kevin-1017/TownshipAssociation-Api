# bcrypt-gen — 管理员口令哈希生成器

本期后端**没有改密接口**(见 docs/API.md「推迟实现」),轮换 `tsa.admin_user.password_hash`
只能:本机生成 BCrypt 哈希 → 到服务器 MySQL 手动 `UPDATE`。本工具就是第一步。

> ⚠️ 哈希在本机离线生成,明文密码不经过网络、不写入任何文件;
> 登录时明文密码仅经 HTTPS 传给后端做 `BCrypt.checkpw` 比对,绝不明文落库。

## 使用(PowerShell)

```powershell
cd C:\Users\KevinH\Programs\Project\TownshipAssociation-Api\tools\bcrypt-gen
npm install          # 仅首次
node gen.mjs "你的新明文密码"
```

输出一行 `$2b$10$...` 即为要入库的哈希。
命令行传参会留在 PowerShell 历史,用完可执行 `Clear-History` 清理。

## 到服务器执行改密

```sql
SET NAMES utf8mb4;
UPDATE tsa.admin_user
SET password_hash = '$2b$10$……上一步输出的整行'
WHERE username = 'admin';
```

幂等说明:`password_hash` 为 72 字节截断的 BCrypt 串,整行(含 `$` 与前导空格之外的所有字符)原样粘贴,不要换行折断。

## 验证

```powershell
curl.exe -X POST https://www.gdutgaginang.cn/tsa/auth/admin-login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"你的新明文密码"}'
```

返回 `code:0` + token 即改密成功;旧密码应返回 1307。

## 口令强度要求

数据库泄露时攻击者只能离线爆破,BCrypt 的代价由**明文强度**决定:

- ✅ 随机 16 位(字母大小写+数字,如 `FvAmJVkTF6ixwTgM` 这类)——实际不可破;
- ❌ 单词、拼音、生日、`admin123`——字典爆破几秒到几小时。

`gen.mjs` 对 <8 位的输入直接报错拒绝。
