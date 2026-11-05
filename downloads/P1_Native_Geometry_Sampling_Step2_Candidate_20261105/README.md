# P1 原生几何采样第二步候选

日期：2026-11-05。独立应用p1.readonly.geometry.step2.P1ReadOnlyGeometrySampler；新命令SISWReadOnlyGeometrySample，列表名Sample Read-Only Geometry。基于已验证Step1扩展，但不覆盖Step1、现有BatchRename/Web、SISWSheetMetalConvert或旧日志。

## 本轮实现

保留Step1模型/单位/精度/体/曲面读取，再对本次模型枚举中真实GetSurfaceType为PLANE/CYLINDER的曲面采集点和法向。UV范围只生成候选；仅接受两次原生VerifyUV返回PLACE_INSIDE、Eval3DData返回UV与请求UV数值精确一致、点/法向有限且法向非零、长度单位已知的样本。边界、域外、异常、返回UV调整、无效向量均不接受。

保存原始法向及归一化法向、原生曲面方向枚举，不自行把法向翻成实体外法向。点坐标按SDK返回的当前零件模型坐标记录，UV参数不是长度单位。每点保留面/体关联、两次原生域内判定、请求/返回UV及来源；这些是SDK原生有效性证据，不是独立几何内核证明。带孔/裁剪区域及周期缝的VerifyUV语义、求值行为仍需真机交叉核验。

每目标面最多25个候选，目标3个有效样本；每轮最多300个目标面、3000次候选尝试、900个接受样本。继承30秒软预算和Step1基础枚举上限，不能中断单次阻塞SDK调用。部分/未尝试对象、拒绝原因、点配额覆盖和分母是否已知均记录，不用少量点宣称整个面的面积覆盖或等厚。

原生GetRevision仍如实读取，返回null就保留null；独立新增Model.GetVersionStamp及来源，前后比对不替代revision、不把遗留严格身份条件改成成功。最终JSONL可能仍为PARTIAL，即使有效采样点配额已满足。

## 交付和验证

source包含3份源码，classes包含8个完整class，lib/p1-readonly-geometry-sampling.jar为相同class薄JAR；注册模板和独立消息资源随包提供。manifest、SHA256SUMS、真实编译/重复编译/SDK签名/字节码/PTC调用清单及纯逻辑测试证据均附带。

真实otk.jar、Java25 javac、--release17、UTF-8、-proc:none、-Xlint:all编译退出0，无警告；另一个干净目录编译class散列相同。class格式major61，但SDK依赖major69，运行需Java25。SDK/JDK及pfcasync.jar不随包分发，未使用pfcasync。

78项纯逻辑/数值检查（19项基础+59项采样数学/门禁）通过，未使用SDK Stub或模型Mock。完整字节码常量池核对46项PTC方法引用，包含lambda引用；UVParams.create/set仅创建和填写求值参数对象，不修改模型参数。没有模型创建/再生/保存/删除/转换、Ribbon写入、HTTP或浏览器桥调用。

没有运行Creo或执行原生采集，没有生成运行requestId/采样成功样例。编译和纯函数测试不证明孔/裁剪域语义、法向坐标方向或实际覆盖率，不能宣称真机通过。

## 日志兼容和使用

Step1源码、二进制、消息资源及历史JSONL不变。新文件geometry-sampling-<真实UUID>.jsonl使用原有基础schema及字段形状，额外标记collectorStep=STEP_2、samplingSchema=P1_NATIVE_GEOMETRY_SAMPLING_V1，并追加采样事件。基础字段名称/类型不改；采样状态字符串按本轮新含义说明，严格按旧枚举验证的消费者不能将新文件冒充旧Step1运行。详见[日志与兼容性](docs/log-contract-and-compatibility.md)。

只读任务不包含厚度/等厚/相对面距离推断，不启用转换或保存按钮。下一步先静态审核，再按独立授权执行[受控验证](docs/controlled-verification.md)。模板占位符不可原样加载，不修改生产配置或持久化Ribbon。
