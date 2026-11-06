# P1 第十三步受控转换阻断证据审核

2026-11-06（Asia/Shanghai）。输入native-step13-blocked-evidence-20261106.zip，仅含一份5行一次锁JSONL，没有原生对话框截图、完整stderr堆栈或加载散列。本次只解析证据、核对已发布候选源码及class身份，没有执行模型API、清锁、重试、改代码或部署。

## 结果

这次是“转换前置检查阻断”，不是“原生转换调用失败”。最终BLOCKED_BEFORE_WRITE_NO_SAVE，writeDispatchIntentRecorded=false；根据这份连续终态日志及对应候选控制流，未到WCreateFeature或显式WRegenerate，不是一次成功或失败的原生创建能力测试。保存和自动回滚标志均false，但不等于对整个Creo会话/其他插件独立认证零写入。

requestId=2f8f2570-3dab-4da2-9022-0b701582ab66。北京时间16:15:48.451首次记录至16:15:48.524最终记录；控制计时至终态约98.533 ms，可见首末记录跨度71.931 ms。这些不包括所有命令准备/结果UI显示，也不是暂停或转换耗时。

| sequence | 记录 | 支持的事实 |
|---|---|---|
| 1 | CLICK_CLAIMED | 一次点击已消耗，创建持久记录 |
| 2 | WFC_INTERNAL_ADAPTER_ACQUIRED | WSolid视图经wfcPart$WPart构造取得 |
| 3 | WFC_INTERNAL_ADAPTER_ACQUIRED | WSession视图取得 |
| 4 | ERROR / CLAIMED | 默认体组合前置条件抛IllegalStateException |
| 5 | FINAL_RESULT | 前写阻断、没有记录提交意图 |

两条WFC适配器记录支持本次取得了对应视图，但自身仍internalApiRuntimeContractCertified=false；不能因此认证所有内部构造契约、许可或转换语义。日志没有PREFLIGHT、第一次风险确认、厚度输入、第二次确认、写提交、创建/再生返回或转换后回读。

## 为什么现在还不能确定具体原因

已发布候选source/P1ControlledConversionStep13.java第114行是：

```java
s.body=s.solid.GetDefaultBody();
if(s.body==null || !same(bodies.get(0),s.body) || s.body.IsSheetmetal())
    throw new IllegalStateException("默认体未知/不一致/已是钣金，不执行。");
```

这条提示合并了默认体为空、列举对象与默认体身份比较失败、或原生已是钣金三种情况，诊断粒度不足，是当前实现的可改进点。same在列举项为空时也返回false；该函数主要检查Java对象相同或非零getptr相同，没有记录本次实际对象/ID/指针比较结果。

Java ||短路：前一项已经成立时，不会再调用后面的IsSheetmetal。因此本轮不能断言nativeIsSheetmetal=true，也不能断言false，更不能据旧Step10日志中的false代填当前状态。若比较失败是不同SDK视图/包装句柄导致的假阴性，现在也没有直接证据证明；getptr不是本次已认证的稳定业务身份契约。不要直接改成比较ID就放行，同ID仍需模型/体作用域依据。

按对应源码，目标零件类型/文件名、WFC适配和单体列表数量门在本错误之前，单位、驱动面、厚度与两个确认在之后。这是源码条件下的路径推断，不是原日志直接提供了那些当前字段；缺少现场加载class/SDK散列，不能将源码比对等同于加载身份认证。更不能把这个阻断解释成“壁厚不合适”“未选驱动面”或“原生转换不支持”，这些阶段尚未到达。

FINAL_RESULT中controlState仍CLAIMED符合当前日志顺序：finish先写记录，再在Java内转FINISHED。不能仅凭该字段认为终态没有处理；反过来，本文件没有close/UI回执记录，也不提供close或结果弹窗独立证明。

## 一次锁与下一步

本次点击资格已消耗。保持原一次锁，不删除、清空、改名、换目录/用户或重载绕过；本次未创建另一转换命令或安排第二次转换。阻断前没有写模型，不需要把“回滚成功”作为结果，也不能悄悄清锁再试。原先唯一一次点击的范围不因未到提交就自动增加。

下一步应先做独立只读诊断，而非重新点击转换：将当前默认体和Model.ListItems(ITEM_BODY)配对记录，分别给出数量/是否为空、公开类型、GetId、模型作用域、原生IsSheetmetal及身份比较的可见结果/错误。默认体、对象比较和钣金状态应拆成独立诊断字段与错误码，避免短路掩盖后项。该诊断不得调用WCreateFeature/WRegenerate/Save，不修改转换锁，也不将其当转换重试。

如果默认体为空，先澄清原生体枚举语义；如果确为已钣金，停止重复转换；如果IsSheetmetal=false但两个视图身份比较失败，应核对SDK提供的体身份契约和模型/体作用域，再决定如何修正检查。不能在无证据时删除安全门或用局部1.2mm推动转换。后续新的转换尝试需由新的受控任务定义，不包含在本次审核内。

本次没有新代码/class/JAR、现场原生执行或模型修改，仅交付审核、分支表和校验。真实转换尚未执行；材料占据、全局壁厚、可转换及生产可发布仍未证明。没有要求为已知缺失的历史过程补造日志/时间，也不根据一个组合错误猜测模型已转换。
