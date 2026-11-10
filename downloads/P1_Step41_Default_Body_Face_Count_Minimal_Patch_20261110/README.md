# Step41：默认体面数量最小只读补丁

日期：2026-11-10（北京时间）。承接 Step40 已验收结果，本轮只新增 p1_readonly_body_face_count。只统计当前零件默认体的面访问次数，不读取面 ID、面类型、几何参数，不识别驱动面，不转换。

## 1. 改了什么文件

| 文件 | 变化 |
| --- | --- |
| src/entry.cpp | 增加 ProSolid.h、默认体面计数回调、第三个命令及相应 JSONL；Step40 命名空间内原有实现逐字保留。 |
| text/p1_readonly_body_face_count.txt | 同一新命令的三个 GUI 指定消息条目。 |
| patches/readonly-body-face-count.diff | 一个补丁，仅修改基线源码并添加新消息文件。 |
| 其他 text 文件、build/required-link-options.rsp | 沿用已通过的消息与固定 plus 链接要求，未修改。 |

基线 Step40 entry.cpp SHA256：6282b445d90af7bd4f40311c8fa41757819db19c2593989e516bb63a83005776。可直接采用完整源码，或在隔离副本核对基线后执行 git apply --unidiff-zero --check，再用 --whitespace=nowarn 应用同一 diff。消息文件四行一条，尾部空行属于格式，不自动清理。

## 2. 为什么改

现场 Step40 验收摘要已确认：无模型分支、零件名称/类型、默认体和特征数组长度均通过，数组释放返回 0。本轮不改这些逻辑，不再重复采集版本或重查已固定的启动问题。

用户补充的同版 SDK 声明是：

```cpp
extern ProError ProSolidBodySurfaceVisit(ProSolidBody *body,
                                        ProSurfaceVisitAction visit_action,
                                        ProAppData app_data);

typedef ProError (*ProSurfaceVisitAction)(ProSurface p_surface,
                                        ProError status,
                                        ProAppData app_data);
```

本版没有这里需要的默认体面数组获取接口。代码不假设存在 ProSolidBodySurfacesGet/Collect，不用 deprecated 的 ProSolidSurfaceVisit；使用默认体专属访问器。回调忽略 surface/status，只把传入的本地 int 计数器加一并返回 PRO_TK_NO_ERROR，不调用 SDK、不记逐面日志、不保留面句柄。

本新命令不分配面数组，因此没有新的 ProArraySizeGet/ProArrayFree。原有模型信息命令的特征数组释放逻辑保持不动，不能因为本轮使用访问器就删掉原逻辑。

## 3. 预期通过时看到什么

本机助手沿用 Step40 已成功构建命令和 SDK Include/Lib，不改入口签名、返回值、CRT 或库组合。保留：

```text
/INCLUDE:user_initialize_plus
/INCLUDE:user_terminate_plus
```

重编新 entry.obj、输出到独立候选位置，记录新 DLL 散列；dumpbin 继续核对 user_initialize_plus、user_terminate_plus，并保留本源码声明的普通入口。沿用既定注册/消息布局，将新消息文件置于已有两个命令消息文件的同一成功解析位置。不要叠加同名命令、覆盖加载中的 DLL 或改 config.pro。

初始化时，在原有两个命令的日志之后应出现：

1. BODY_FACE_COUNT_REGISTER_BEGIN。
2. BODY_FACE_COUNT_ACTION_ADD_RETURN，pro_error=0。
3. BODY_FACE_COUNT_DESIGNATE_RETURN，pro_error=0。
4. INIT_BEFORE_RETURN，return_code=0。

用户从准备好的 GUI 入口点击一次，应新增一条 BODY_FACE_COUNT_ENTER 和一条 BODY_FACE_COUNT_RETURN。仍使用 p1-step35-<PID>.jsonl、原 tag、UTC、load_token、seq；没有新增日志目录或通道。指定成功仅使命令可绑定，不自动创建 Ribbon 按钮，本机助手按已经通过的方式准备必要绑定，不让用户调代码。

| 情况 | 新命令结果 |
| --- | --- |
| 无活动模型，当前查询为 PRO_TK_BAD_CONTEXT | result=no_active_model、no_active_model=true；不查询类型/默认体或访问面，face_count=null。 |
| 当前不是 PRO_MDL_PART | result=non_part_model；不转换 ProSolid、不访问面，face_count=null。 |
| 默认体查询为本版约定的 PRO_TK_GENERAL_ERROR | result=no_default_body、default_body_present=false；不访问面，face_count=null。 |
| 默认体面访问返回 PRO_TK_NO_ERROR | result=ok、default_body_present=true、face_count 为实际访问次数，surface_visit_pro_error=0。 |
| 返回 PRO_TK_E_NOT_FOUND 且回调次数为 0 | result=no_faces、default_body_present=true、face_count=0。 |

face_count 的口径是默认体访问器实际回调次数，不是特征数。Step40 的 feature_count=1 不能拿来当本轮预期面数。新命令不重新读取名称、特征数或其他模型信息。

只安排必要输入验证：无模型可点击一次；对用户已有的活动零件可再点击一次。插件不为了验收打开模型，不要求重复点击旧命令。

## 4. 预期失败时如何分支

- 编译/链接失败：本机助手回传首个完整错误，不自行编写核心 C++、猜函数名或强转回调。ProSurfaceVisitAction 直接接收函数指针，真实 SDK 类型参与检查。
- 注册/指定非 0：停在相应阶段，核对新资源和实际错误；DLL 正在运行不等于第三个命令通过。
- 当前模型、类型或默认体查询出现其他错误：保留相应 pro_error；没查询/没完成的字段为 null，不写成无模型、无默认体或零面。
- 面访问返回其他错误：result=surface_visit_failed，face_count=null，仅 visited_count_before_error 记录已访问次数。该值不是完整计数，不自动重试。
- 如果返回 E_NOT_FOUND 却已经访问过面：同样按失败/不一致保留实际码和部分次数，不强行改成 0。
- 只有 ENTER 或缺少日志：先核对新 DLL 身份、实际点击入口和既有日志通道，不从缺记录直接断言没有进入。

UI 命令回调仍返回 0，数据验收看 BODY_FACE_COUNT_RETURN.result 及各 SDK 返回码。先前只读模型信息功能已验收，不在本轮重复重构或推翻；终止/OS 卸载依旧不能从初始化日志直接推导。

## 5. 不做什么

不打开或选择模型/面/特征，不取面 ID、类型、方向、参数或坐标，不做几何判定，不缓存面句柄，不查特征内容，不进入写操作，不创建、不保存、不转换。不修改入口签名、plus 链接、CRT、DAT 结构或 config.pro，不自动启动 Creo，不增加 DllMain。

本包只交付一个功能补丁及必要消息资源。云端只做基线/补丁应用及静态调用范围检查，没有真实 SDK 编译、访问器运行或 GUI 验证，没有 SDK Stub。实际构建、导出核对和一次最小受控测试仍由本机助手按 SOP 完成。
