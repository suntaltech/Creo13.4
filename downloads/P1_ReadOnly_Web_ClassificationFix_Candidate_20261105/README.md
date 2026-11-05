# 只读 Web 分析误导分类最小候选

日期：2026-11-05。基于用户本次上传的3份实际Web文件，不以历史源码覆盖未知基线。本包是供离线审核与后续受控验证的Web补丁，不是安装包或已部署版本。

## 修复了什么

删除以feature.type文本判断原生钣金、以最小包围跨度估算壁厚及估算失败默认shell的推断。原生钣金状态显示未核验，薄壁形态与等厚状态显示证据不足；estimatedThickness和suggestedMode均为null，不错误推荐抽壳，也不反向默认推荐driving。

保留模型、曲面数量、有效曲面面积及包围尺寸读取，移除“候选即平面”的称谓。新增独立纯评估模块sheetmetal-assessment.js，HTML按顺序加载。单位来自file/get_length_units；未取得时显示未确认，不硬编码mm。用户板厚参数不当测量结果，控件保持禁用，方式不默认选中，保存复选框始终不选中。

分析读取顺序改为fileinfo、surfaces、bound_box、units、fileinfo；曲面/包围盒/单位显式指定最初模型。移除无助于本轮结论的feature/list读取。前后file/revision变化则失败；两次都缺revision时只声明文件级核对，不伪装完整一致性。单位读取失败允许显示其他读取数据，但保留警告。

新增一次分析的requestId、开始/逐读取/结果/失败审计，以及下载分析日志按钮。ID只有实际点击才生成，本包未填真机日志。导出只选smt_*事件和已知字段，并脱敏当前连接值；共享core.js完全不改，不影响Rename/About使用的公共接口。共享core仍有原有连接事件和通用bridgeCall，补丁不调用bridgeCall，也不声称全局core已被改成只读。不要使用旧core.downloadLog导出完整共享事件后直接发布，其中可能含连接凭据。

## 交付范围

web/提供sheetmetal.html、assets/js/sheetmetal.js和新增assessment模块；core.js只附原字节参考并在manifest标记不变，审核应用时无需覆盖。patches/提供针对明确源文件散列的完整补丁。Java/classes/JAR、R8消息资源和Ribbon均无需改变，继续沿用已验证导航诊断二进制。

当前没有本案例原始HTTP响应、真实法向/相对面距离或厚度样本；因此本候选仅完成“去除无依据结论、明确未知及日志”的第一阶段，不宣称识别出等厚薄板、已是钣金形态、可展或转换可行。不是用unknown替代几何算法后宣称算法完成。

## 验证

13项测试在Node中执行真实纯评估/日志筛选函数，使用明确标注的合成输入，覆盖大小/轴排列不影响未知状态、特征名不决定状态、无效响应、单位和日志凭据排除。3份JS语法检查、HTML依赖顺序/禁用控件核对、补丁精确重放、core散列不变检查通过。没有运行浏览器、HTTP、Mock/Stub服务器或Creo；没有真机通过证据。

SHA256SUMS覆盖交付文件；manifest记录输入与输出文件身份，evidence保留真实测试输出。tests不是本机采集的曲面数据，不作为几何识别验证。

后续请按docs/controlled-verification.md先审核实际Web散列与依赖，再在单独授权下验证页面。只替换适用的Web文件，不改共享core、其他页面、Java、SDK、生产配置或模型。原始Web不是完整站点包，base.css等站点依赖未提供，本候选沿用既有站点依赖。
