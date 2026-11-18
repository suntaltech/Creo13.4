# P1 Step41 C++ 6A R2-B 单位探针真机观察复核回执

2026-11-18 · Creo Parametric 13.4.0.0 x64 · 受控只读探针

## 复核结论

接收本机观察报告与回填表，支持本次会话中 **mm→mm 单位反馈和创建前硬停**的观察结论。两份材料逐项对账为 18 个观察单元，其中 12 个由现场标为 VERIFIED、6 个为 UNVERIFIED。部署 DLL 的回填大小与 SHA-256 和离线验收记录一致。

本轮复核比较的是报告、回填表和已归档的离线验收记录。原始 JSONL、各组机读记录、截图及回滚记录未随本次回传提供，因此事件、界面和回滚状态均按现场回填引用，未从原始证据独立重放。

| 观察 | 复核结果 |
|---|---|
| 注册和可见性 | 现场报告称探针在当次定制列表可见，ActionAdd 和 Designate 返回成功。探针无独立 Find 回查，保留为空。 |
| 守卫与拒绝 | 已回填无模型、装配拒绝、已是钣金、非平面以及面归属守卫。面归属用例直接支持 `ProSolidBodySurfaceVisit` 返回 `E_NOT_FOUND` 并走 `FACE_BODY_MISMATCH` 分支。 |
| 取消 | 选面返回 `PRO_TK_USER_ABORT`（-3），板厚返回 `PRO_TK_MSG_USER_QUIT`（-31），终态均为 `CANCELLED`。 |
| 成功样本 | op9 的 `SMT_CONV_UNITS`（seq226）与 `SMT_CONV_RETURN`（seq230）在回填材料中配对。输入 1 mm，当前长度单位 mm，`scale=1`、`offset=0`，终态 `UNIT_PROBE_STOPPED`，创建、重生成标记均为 `false/null`，修改及保存标记为 `false`。 |
| 同会话兼容 | 探针执行后，ping、model_info、body_face_count 均有成功回填。body_face_count 在已放置的 QAT 控件可触发，现场记录当时的左侧候选列表过滤条件下未显示该命令。 |
| 收尾 | 现场报告记录 Creo 会话结束、插件退出收尾，以及 `config.pro` 回滚并移除探针配置行。 |

## 保留事项

- 单位观察只覆盖 mm→mm 同单位样本。该样本的 `scale=1` 对正反方向均成立，因此尚不能验证 mm→inch 等异单位换算方向；模型单位名称解析也只覆盖当前样本。
- 自定义中文状态栏消息现场显示乱码，SDK 自带范围提示显示正常。自定义消息的 API 成功状态与实际显示须分别判定。现有记录未定位具体编码或解码环节，也未验证编号串替换。
- 六个未验证单元为 `no-body`、`no-face`、`ambiguous-face`、`non-surface`、`invalid-thickness`、`unit-unsupported`。缺样本和现场 SDK 前置行为解释了本次未覆盖，不能据此推定分支在所有真机场景下不可达。具体而言，越界输入 25 在本次被 SDK 拦截，取消时返回 -31。本用例未验证探针自身的 `INVALID_THICKNESS`，也未触发 `INPUT_FORMAT_FAILED`。
- 预选缓冲数量和生命周期仍待验证。本轮结论覆盖 B 类只读探针；正向创建、重生成和保存不在本次验收范围。
