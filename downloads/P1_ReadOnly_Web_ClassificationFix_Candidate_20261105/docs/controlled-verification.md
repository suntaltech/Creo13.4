# 后续本机审核和受控验证清单

本轮只生成离线候选，以下不是GUI或部署授权。不得自动替换生产Web、重启进程、点击模型操作或恢复配置。

## 基线与应用范围

先核对实际sheetmetal.html、sheetmetal.js、core.js是否仍等于manifest.inputs；不一致时停止，保留差分，不强制覆盖。仅应用HTML、sheetmetal.js与新增sheetmetal-assessment.js三项；core.js是原样参考，不应用任何修改。脚本须按core→assessment→sheetmetal顺序加载；静态审核缺失新模块时不得继续点击分析。

Java无需重编译。该Web候选与前一导航诊断class身份分别记录，不能将Web分析ID当作PAGE_NAVIGATION ID。页面缓存和实际加载文件身份要确认，不能因磁盘有新文件就认定浏览器已加载补丁。

## 预期页面与分析行为

页面标题为“零件几何只读分析”，转换方式无默认选项、参数禁用、保存未选中；转换按钮保持禁用。打开solid_part入口后允许点击分析；其他未确认入口阻止分析。已知钣金入口仅作入口报告，不自行宣称原生状态已重新读取。

本案例可继续显示真实读取到的曲面数、有效曲面面积和包围尺寸，但“普通实体→抽壳”文案应消失。钣金原生状态未核验，薄壁形态/等厚为证据不足，厚度未测量，转换方式未推荐。若仍显示旧文案先核对缓存与文件身份，不点转换。

若单位接口返回字符串，则按返回单位显示；接口不支持或响应不符时记录smt_units_unconfirmed并显示未确认，绝不默认为mm。file/revision前后不一致时显示MODEL_CHANGED；两次revision都未提供时显示核对范围有限。缺失曲面列表或无效包围值显示失败，不伪造空列表成功；零曲面响应仍可显示0，不据此证明实体种类。

## 允许读取与审计

本模块业务请求仅file/get_fileinfo、geometry/get_surfaces、geometry/bound_box、file/get_length_units；通用core可能为读取建立connection/connect，网络不确定时重连并重放一次读取。一次smt_read_started不等于一次网络请求，核对实际HTTP command/function/data。

浏览器SiswCore.events中预期smt_mode_guard、smt_analysis_started、smt_read_started/passed/failed、必要时smt_units_unconfirmed，最后为smt_analyze_http或smt_analyze_failed。每次真实分析ID贯穿读取事件；页面回调ID另行关联。成功摘要含model/revision/units、surfaceCount、原生/形态/等厚状态、reasonCodes、revisionCheck和unitReadOutcome；suggestedMode、estimatedThickness、planeCount均为null，nativeConvertReady=false、saved=false。

这些是未来预期，不是本候选生成的真机结果。saved:false只表示该分析代码没有调用保存，不证明整个Creo进程没有其他写操作。下载分析日志保存本地JSONL证据文件，不保存模型；下载动作本身不是模型写入。请同时留存脱敏原始HTTP响应、实际源文件散列、模型前后观察及页面截图，日志下载内容只含允许字段，不包含原始几何响应或所有HTTP尝试。

转换按钮即使异常触发其处理器，也只记录smt_convert_blocked，不调用HTTP/native bridge；不要为了测试它而强行启用控件或修改DOM。本模块没有新增实际写模型接口。

出现未知/写请求、模型dirty或特征变化、错误提示、卡顿等情况时，停止该次观察并保留证据；不要用Save、Undo、删除或自动重试掩盖变化。MODEL_CHANGED报错不自动重新分析。当前file/revision一致不能排除中途切换再切回或其他未观测变化。

## 下一阶段输入

要进一步判定STP导入薄壁/等厚，需要真实曲面类型、法向、相对面重叠及距离/厚度样本等经核实读取能力。当前没有这些数据，也没有新增SDK几何接口，不通过轴向extent、面积比或全局最小跨度猜出“等厚”。先完成本次纠偏真机验证，再依据脱敏响应制定几何证据采集方案。
