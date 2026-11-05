# 导航日志与验收口径

本候选保留GetURL、原SetURL比较及同页跳过行为；不尝试消除PTC异常。它澄清失败阶段，而非宣称修复底层GetURL原因。所有新增事件为未来候选执行时预期，不是本轮产生的真机日志。

日志格式为既有时间前缀 + PAGE_NAVIGATION + JSON。JSON携带requestId（来自既有回调ThreadLocal）、commandId、phase、outcome、targetUrl、preNavigationUrl、pageLoad=UNVERIFIED；异常附exceptionClass并由既有logger打印真实堆栈。不得从日志中复制ID去填另一请求。

| 情形 | 预期事件与行为 |
|---|---|
| GetURL正常返回URL/null | PRE_NAVIGATION_URL_PROBE ATTEMPT、RETURNED；preNavigationUrl为导航前值，null不等于异常 |
| GetURL抛异常 | PRE_NAVIGATION_URL_PROBE THROWN + 真实堆栈；保持原null策略继续尝试SetURL，不标成SET_URL失败 |
| 导航前URL等于目标 | SET_URL SKIPPED_SAME_URL，保留原有不重复SetURL的行为 |
| 导航前URL不同或为空 | SET_URL ATTEMPT；API返回后RETURNED；不据此声称页面已加载 |
| SetURL抛异常 | SET_URL THROWN + 堆栈，failedPhase=SET_URL；不生成回调成功事件 |
| 窗口尺寸/激活/刷新失败 | 对应SET_BROWSER_SIZE/ACTIVATE/REFRESH THROWN及真实堆栈；不归因给URL探测 |
| 导航流程API调用均返回 | CALLBACK_COMPLETION RETURNED；旧人读日志改为preNavigationUrl、target及pageLoad=UNVERIFIED |

窗口获取/列表回退仍沿用原日志。窗口不存在时原逻辑直接退出并记录No available Creo window，不生成导航完成；被inactive/opening/session检查阻断的回调也不生成上述导航完成事件。结构化日志未给这些既有路径补造成功。

同页再次点击仍会设置浏览器大小、激活和刷新；只跳过SetURL，与R8原行为相同。GetURL失败时仍会重复SetURL，这不是新增行为。页面是否显示需要外部截图/观察补证；pageLoad字段始终UNVERIFIED。preNavigationUrl不是导航后读回。

未来编译/受控回调验证至少覆盖已知返回URL、返回null/抛异常时的实际可观察情况、同页跳过及不同页导航；不能人为触发写模型失败来测试日志，也不要求为了覆盖异常而制造SDK异常。尚未执行的分支保留未验证。测试范围只含页面命令，不点SISWSheetMetalConvert，不Add或保存布局。
