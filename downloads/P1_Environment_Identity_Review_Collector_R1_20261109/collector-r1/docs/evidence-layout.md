# 现场输出结构与字段判读

```text
P1_EnvIdentity_<UTC>_<runid>/
  collector-manifest.json
  installation-images.json
  processes-and-jvm.json
  sdk-identities.json
  plugin-identities.json
  configuration-index.json
  configuration/
    0001_config.sanitized.txt
    0002_dat.sanitized.txt
    ...
  log-summaries.json
  scan-coverage.json
  SHA256SUMS
P1_EnvIdentity_<UTC>_<runid>.zip
P1_EnvIdentity_<UTC>_<runid>.zip.sha256
```

| 请求字段 | 输出位置 | 证据界限 |
|---|---|---|
| Creo安装路径/维护版本/进程路径 | installation-images.json与processes-and-jvm.json | 安装目录候选与OS映像路径/文件版本分开；无法读取则未知，不能用文件夹名代替维护版本 |
| otk.jar、pfcasync.jar路径/大小/SHA256 | sdk-identities.json | 显式路径、安装目录或dat候选；loadedIdentityProven=false，存在两个JAR不等于运行态混装 |
| 插件目录、class/JAR身份 | plugin-identities.json与configuration-index.json | classpath配置/目录内文件哈希，不复制内容；未证明已加载磁盘同一版本 |
| config.pro全文 | configuration/*.sanitized.txt | 全行序列的脱敏/归一化文本，非环境值及普通注释遮盖；原始读取字节SHA在index |
| 启用/注释protk dat | configuration-index.json的datReferences、dat脱敏快照 | commentedInThisFile区分当前文件中的注释；并非全局活动/加载认证，未找到/解码失败/范围限制在coverage |
| JVM版本/供应商/位数 | processes-and-jvm.json的jvmEvidence | 已映射jvm.dll文件和附近release限定字段，不调用实时JVM API、不把采集器PowerShell或系统默认Java当宿主JVM |
| 启动classpath | process classpathArguments、dat javaAppClasspaths、配置快照 | 命令行/配置来源；缺少嵌入式JVM类路径时不猜测 |
| 模块加载先后 | log-summaries.json的existingClassLoadEntries | 仅已有日志的局部行顺序；DLL枚举顺序明确不是加载时间 |
| jlink/stderr/crash摘要 | log-summaries.json | 限定错误标记计数、识别的类加载记录、JSONL规范事件计数/末事件，无原始行、栈地址、坐标或句柄 |
| 覆盖率/失败 | scan-coverage.json、collector-manifest.json | 有限制/缺项就是缺项，ZIP生成不等于采集完整；不自动给出许可或路线PASS |

文本读取采用共享只读打开，不锁住或修改源文件；运行期间文件变化会标注stableDuringRead=false，散列代表实际读取的字节，不应当作静态原件认证。没有读取权限时只记录异常类型和缺项，不尝试提权、重试或改变ACL。

目录扫描只在明确目录或由配置/当前映像推断的候选内进行，拒绝盘符/用户主目录，不跟随链接；有条目/目录上限。不是全磁盘搜集器，不能证明已找到所有配置。可能的config.sup/多级config.pro和启动时覆盖关系需结合既有启动记录，由后续离线审核说明。

日志只接受指定文本文件，超限明确跳过；不收集二进制dump、不扫描模型文件内容。已有日志可能属于旧会话，associationToCurrentSession保持未知，需靠PID/启动时间/现场来源关联。没有错误标记不证明未出错，最后一个JSONL事件也不独立证明崩溃原因。

当前配置、SDK、JVM与许可核对完成后，再按docs/offline-review-checklist.md判断是否混装、注册模式/许可条件是否吻合、内部WFC桥接是否有官方依据，以及Java/C++路线。不得直接据文件存在或枚举成功恢复转换。
