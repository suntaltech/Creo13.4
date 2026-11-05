# G1 / C1、C2 清单离线衔接审核

日期：2026-11-05。审核对象为用户提供的《P1_R7_G1_C1C2资源观察_受控操作清单_20261105.md》。附件是待审核操作材料；本次离线处理及上传授权不代表 GUI、重启、dat 切换或模型操作授权。

结论：观察目标和停止边界可保留；以下问题应在受控操作前澄清。本次没有执行 C1/C2，也没有生成或克隆运行记录、证据目录、截图、日志、requestId、ZIP。原附件、R7 候选、schema 和空白台账未改。

## 1. 计划值与实际值分开

第7、8节 selectedLayout 已分别预填 root-layout/usascii-layout，与“全部留空”不一致。plannedLayout 保留计划值；未执行的 selectedLayout 应为 UNVERIFIED，确认真实加载资源后再填。actualRestartObserved、actualStartup、身份散列和模式同样不得由计划推定。命令已注册也不证明资源目录实际生效。

## 2. 单变量比较需要限定字段

第4节“唯一允许差异 selectedLayout；其他字段不一致即变量污染”应改为：比较条件只主动改变资源布局；planId、plannedLayout、会话标识、时间、文件名、证据散列及实际观察结果允许且通常必须不同。C1/C2 是两个独立重启会话，以外部配对上下文归为同组；不得共用 sessionId 或截图。

应固定并核对源码身份、实际加载 JAR/class 身份、dat 字节、classpath、启动方式、Creo版本、语言、测试模型内容与模式、操作者、搜索流程及其他加载配置。日期跨日需说明，不能为满足“同日期”改写真日期。若 text_dir/dat 也发生变化，必须记录为额外变量；不能继续声称只改变布局。方案可保持同一 text_dir，离线切换其受控资源树，但本次不执行此操作。

即使控制条件一致，两次顺序观察仍只支持“所记录条件下的可见性差异”；不能仅凭一次 C1/C2 比较断言所有差异只能由 layout 引起。

## 3. 每个搜索范围分别记录

4命令 × 当前/所有模式 × 2布局，共16个计划观察单元。若候选列表与命令搜索是不同界面，还须分别标记界面，不混为同一个结果。每个单元从原 manualUiObservation 空对象独立克隆，在 searchCategory 中记录实际范围/界面，以外部上下文关联会话、模型模式、布局与尝试序号。

附件的每命令单一 l3CandidateListVisible 无法区分两个范围：不要合并一次命中和另一次未命中。无法打开某范围时保留实际阻塞原因，不制造“未找到”截图或结果。

## 4. 展示模板不是 schema 记录

附件第7、8节把会话、commands 数组、requestIds 和简化命令对象合在一起，可作为外部操作表，但不能直接当成既有 manualUiObservation 提交。该定义禁止额外字段，且要求 recordType、source、searchCategory、L4相关字段、证据真实性字段等；不要扩展冻结 schema 来迎合此表。

规范落点仍为：sessionPlans 对应会话；manualUiObservation 对应单命令/范围观察；evidenceCustodyRecord 对应具体 case/session/attempt 和 commandId 的关联核对。一个会话多条观察需分别关联，不用一条单 commandId 的保管记录冒充四命令全部核对通过。纯列表观察 requestIds 可为空；不为未发生回调造 ID。

sourceSha256 表示源码身份，不等于 JAR、class 或资源身份。外部受控身份说明分别记录并引用这些材料及散列，再关联到会话/保管记录；不将 JAR hash 填进源码 hash 字段，也不擅自在 additionalProperties=false 的对象加字段。

## 5. 证据命名和判定

保留既有五段命名；condition 必须区分 layout、搜索范围/界面、sessionScope 与 attemptOrdinal，避免同命令重复搜索覆盖证据。这里只说明规则，不分配真实会话、日期或操作者值。

NOT_FOUND 仅为外部观察描述，可写入 insufficientEvidenceReasons；状态枚举仍为 UNVERIFIED/PASS/FAILED/BLOCKED。真实证据充分、满足既有规则时可评定该具体列表观察的 L3；“搜索词命中”或单份截图本身不自动证明整个 L3 门禁通过，不推出 L4、可点击回调或转换可用。清单第9节“不能据此宣称 L3 通过”应理解为禁止将局部候选命中自动升级为整体门禁结论，而非永久禁止按规则评定具体观察。

不执行 Add、命令点击、转换、WCreateFeature/WRegenerate/Save。误触或禁止调用实际发生时，停止并隔离记录，保留真实请求、阶段和现象；没有 requestId 则如实说明。未观测到禁止调用也不等于已证明从未调用。卡住时保留实际现象，不为采集证据继续操作。

## 6. 下一步范围

本说明仅完成离线审核与衔接整理。后续操作者应先确认上述修订口径，再在另行授权的本机 GUI 会话中执行观察。云端未验证 Creo、配置、模型或运行证据；没有填入运行 PASS。
