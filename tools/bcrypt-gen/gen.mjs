// BCrypt 哈希生成器 — 手动轮换 admin_user.password_hash(本期无改密接口)。
// 首次使用先在本目录执行 npm install。
// 用法:  node gen.mjs "<明文密码>"
// 输出 $2b$10$... 一行哈希,填进:
//   UPDATE tsa.admin_user SET password_hash = "<哈希>" WHERE username = "admin";
// 提示: $2b 与库里旧 $2a 前缀算法等价,Spring BCryptPasswordEncoder 都认。
// 提示: 命令行传参会留在 shell 历史,用完可 Clear-History(PowerShell)。
import bcrypt from "bcryptjs";

const pwd = process.argv[2];
if (!pwd || pwd.length < 8) {
  console.error("[错误] 请传入至少 8 位的密码;建议 16 位随机字母+数字+符号,弱口令会被离线爆破。");
  process.exit(1);
}
console.log(bcrypt.hashSync(pwd, 10));